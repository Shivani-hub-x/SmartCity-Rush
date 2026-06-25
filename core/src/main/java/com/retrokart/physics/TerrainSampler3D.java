package com.retrokart.physics;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

/**
 * TerrainSampler3D
 *
 * Builds a CPU-side triangle list from the track Model at startup,
 * then answers height and surface-type queries via downward raycasting.
 *
 * Only roughly-horizontal triangles (normal.y > 0.5) within a
 * sane Y range are retained, so we skip walls, trees, rocks etc.
 *
 * A simple spatial grid accelerates lookup from O(N) to O(1).
 */
public class TerrainSampler3D {

    // ── Triangle store (flat float[]: x0,y0,z0, x1,y1,z1, x2,y2,z2 ...) ──
    private float[] tris;
    private int     triCount;

    // ── Spatial grid ────────────────────────────────────────────────────────
    private static final int   GRID = 64;       // cells per axis
    private int[][]            gridCount;       // how many tris in each cell
    private int[][][]          gridTris;        // tri indices per cell
    private static final int   CELL_CAP = 256; // max tris per cell

    private float gridMinX, gridMinZ, gridCellW, gridCellH;

    // ── Raycast helpers (reused to avoid GC) ────────────────────────────────
    private final Ray     ray  = new Ray();
    private final Vector3 v0   = new Vector3();
    private final Vector3 v1   = new Vector3();
    private final Vector3 v2   = new Vector3();
    private final Vector3 hit  = new Vector3();
    private final Vector3 tmp  = new Vector3();

    private static final float RAY_FROM_Y = 500f;
    private static final float FALLBACK_Y = -9999f;

    // ── Constructor ─────────────────────────────────────────────────────────

    public TerrainSampler3D(Model model, Matrix4 transform) {
        extractAndFilter(model, transform);
        buildGrid();
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /** Y height of terrain at (worldX, worldZ). Returns 0 if no hit. */
    public float getHeight(float worldX, float worldZ) {
        ray.set(worldX, RAY_FROM_Y, worldZ, 0f, -1f, 0f);

        int cx = gridCell(worldX, gridMinX, gridCellW);
        int cz = gridCell(worldZ, gridMinZ, gridCellH);

        if (cx < 0 || cx >= GRID || cz < 0 || cz >= GRID) {
            return bruteForceSample(worldX, worldZ);
        }

        float bestY = Float.NEGATIVE_INFINITY;
        boolean found = false;
        int count = gridCount[cx][cz];

        for (int i = 0; i < count; i++) {
            int ti = gridTris[cx][cz][i];
            int b  = ti * 9;
            v0.set(tris[b],   tris[b+1], tris[b+2]);
            v1.set(tris[b+3], tris[b+4], tris[b+5]);
            v2.set(tris[b+6], tris[b+7], tris[b+8]);

            if (Intersector.intersectRayTriangle(ray, v0, v1, v2, hit)) {
                if (hit.y > bestY) { bestY = hit.y; found = true; }
            }
        }
        return found ? bestY : FALLBACK_Y;
    }

    /** Surface type at (worldX, worldZ). */
    public TrackSurface getSurface(float worldX, float worldZ) {
        float y = getHeight(worldX, worldZ);
        // FALLBACK_Y (-9999) means no ground triangle was hit → truly off-track
        // Any valid hit means the kart is over some part of the track model → ROAD
        if (y <= FALLBACK_Y + 1f) return TrackSurface.OFFTRACK;
        return TrackSurface.ROAD;
    }

    /** Returns true if there is ANY ground geometry at (worldX, worldZ). */
    public boolean hasGround(float worldX, float worldZ) {
        return getHeight(worldX, worldZ) > FALLBACK_Y + 1f;
    }

    // ── Extraction ──────────────────────────────────────────────────────────

    private float[] extractBuf;

    private void extractAndFilter(Model model, Matrix4 transform) {
        extractBuf = new float[1024 * 9];
        triCount = 0;

        for (com.badlogic.gdx.graphics.g3d.model.Node node : model.nodes) {
            processNode(node, transform);
        }

        tris = new float[triCount * 9];
        System.arraycopy(extractBuf, 0, tris, 0, triCount * 9);
        extractBuf = null;
        System.out.println("[TerrainSampler3D] " + triCount + " ground triangles extracted");
    }

    private void processNode(com.badlogic.gdx.graphics.g3d.model.Node node, Matrix4 transform) {
        Matrix4 globalTransform = new Matrix4(transform).mul(node.globalTransform);

        for (com.badlogic.gdx.graphics.g3d.model.NodePart part : node.parts) {
            if (part.material != null && part.material.id != null) {
                String matId = part.material.id.toLowerCase();
                // Completely exclude the water from physics so the map has real physical bounds!
                if (matId.contains("water")) continue; 
            }

            Mesh mesh = part.meshPart.mesh;
            VertexAttribute posAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Position);
            if (posAttr == null) continue;

            int stride = mesh.getVertexSize() / 4;
            int posOff = posAttr.offset / 4;
            
            float[] verts = new float[mesh.getNumVertices() * stride];
            short[] indices = new short[mesh.getNumIndices()];
            mesh.getVertices(verts);
            mesh.getIndices(indices);

            int startIdx = part.meshPart.offset;
            int numIdx = part.meshPart.size;

            for (int i = startIdx; i + 2 < startIdx + numIdx; i += 3) {
                int a = (indices[i]   & 0xFFFF) * stride + posOff;
                int b = (indices[i+1] & 0xFFFF) * stride + posOff;
                int c = (indices[i+2] & 0xFFFF) * stride + posOff;
                if (a+2 >= verts.length || b+2 >= verts.length || c+2 >= verts.length)
                    continue;

                v0.set(verts[a], verts[a+1], verts[a+2]).mul(globalTransform);
                v1.set(verts[b], verts[b+1], verts[b+2]).mul(globalTransform);
                v2.set(verts[c], verts[c+1], verts[c+2]).mul(globalTransform);

                tmp.set(v1).sub(v0);
                Vector3 edge2 = new Vector3(v2).sub(v0);
                Vector3 normal = tmp.crs(edge2).nor();
                
                // Exclude walls from floor mesh
                if (normal.y < 0.4f) continue;

                float yAvg = (v0.y + v1.y + v2.y) / 3f;
                // Sensible Y band
                if (yAvg < -60f || yAvg > 60f) continue;

                if ((triCount + 1) * 9 > extractBuf.length) {
                    float[] nb = new float[extractBuf.length * 2];
                    System.arraycopy(extractBuf, 0, nb, 0, extractBuf.length);
                    extractBuf = nb;
                }

                int base = triCount * 9;
                extractBuf[base]   = v0.x; extractBuf[base+1] = v0.y; extractBuf[base+2] = v0.z;
                extractBuf[base+3] = v1.x; extractBuf[base+4] = v1.y; extractBuf[base+5] = v1.z;
                extractBuf[base+6] = v2.x; extractBuf[base+7] = v2.y; extractBuf[base+8] = v2.z;
                triCount++;
            }
        }
        
        if (node.hasChildren()) {
            for (com.badlogic.gdx.graphics.g3d.model.Node child : node.getChildren()) {
                processNode(child, transform);
            }
        }
    }

    // ── Grid ────────────────────────────────────────────────────────────────

    private void buildGrid() {
        if (triCount == 0) return;

        // Find XZ bounds
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < triCount; i++) {
            int b = i * 9;
            for (int v = 0; v < 3; v++) {
                float x = tris[b + v*3];
                float z = tris[b + v*3 + 2];
                if (x < minX) minX = x; if (x > maxX) maxX = x;
                if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
            }
        }

        gridMinX  = minX - 1f;
        gridMinZ  = minZ - 1f;
        gridCellW = (maxX - minX + 2f) / GRID;
        gridCellH = (maxZ - minZ + 2f) / GRID;

        gridCount = new int[GRID][GRID];
        gridTris  = new int[GRID][GRID][CELL_CAP];

        for (int ti = 0; ti < triCount; ti++) {
            int b = ti * 9;
            // AABB of triangle in grid space
            float txMin = Math.min(tris[b], Math.min(tris[b+3], tris[b+6]));
            float txMax = Math.max(tris[b], Math.max(tris[b+3], tris[b+6]));
            float tzMin = Math.min(tris[b+2], Math.min(tris[b+5], tris[b+8]));
            float tzMax = Math.max(tris[b+2], Math.max(tris[b+5], tris[b+8]));

            int cxMin = Math.max(0, gridCell(txMin, gridMinX, gridCellW));
            int cxMax = Math.min(GRID-1, gridCell(txMax, gridMinX, gridCellW));
            int czMin = Math.max(0, gridCell(tzMin, gridMinZ, gridCellH));
            int czMax = Math.min(GRID-1, gridCell(tzMax, gridMinZ, gridCellH));

            for (int cx = cxMin; cx <= cxMax; cx++) {
                for (int cz = czMin; cz <= czMax; cz++) {
                    int cnt = gridCount[cx][cz];
                    if (cnt < CELL_CAP) {
                        gridTris[cx][cz][cnt] = ti;
                        gridCount[cx][cz]++;
                    }
                }
            }
        }
        System.out.println("[TerrainSampler3D] Grid built  " + GRID + "x" + GRID +
                " cells, cell size (" + String.format("%.1f", gridCellW) +
                " x " + String.format("%.1f", gridCellH) + ")");
    }

    private static int gridCell(float v, float min, float cellSize) {
        return (int)((v - min) / cellSize);
    }

    private float bruteForceSample(float worldX, float worldZ) {
        ray.set(worldX, RAY_FROM_Y, worldZ, 0f, -1f, 0f);
        float bestY = Float.NEGATIVE_INFINITY;
        boolean found = false;
        for (int i = 0; i < triCount; i++) {
            int b = i * 9;
            v0.set(tris[b],   tris[b+1], tris[b+2]);
            v1.set(tris[b+3], tris[b+4], tris[b+5]);
            v2.set(tris[b+6], tris[b+7], tris[b+8]);
            if (Intersector.intersectRayTriangle(ray, v0, v1, v2, hit) && hit.y > bestY) {
                bestY = hit.y; found = true;
            }
        }
        return found ? bestY : FALLBACK_Y;
    }
}
