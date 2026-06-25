package com.retrokart.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

/**
 * Lightweight 3D-to-2D particle system.
 * Uses parallel arrays to avoid object allocation.
 */
public class ParticleSystem {
    private static final int MAX_PARTICLES = 500;

    private final float[] px = new float[MAX_PARTICLES];
    private final float[] py = new float[MAX_PARTICLES];
    private final float[] pz = new float[MAX_PARTICLES];
    private final float[] vx = new float[MAX_PARTICLES];
    private final float[] vy = new float[MAX_PARTICLES];
    private final float[] vz = new float[MAX_PARTICLES];
    private final float[] size = new float[MAX_PARTICLES];
    private final float[] colorR = new float[MAX_PARTICLES];
    private final float[] colorG = new float[MAX_PARTICLES];
    private final float[] colorB = new float[MAX_PARTICLES];
    private final float[] colorA = new float[MAX_PARTICLES];
    private final float[] life = new float[MAX_PARTICLES];
    private final float[] maxLife = new float[MAX_PARTICLES];

    private int count = 0;
    private final Vector3 proj = new Vector3();
    private final ShapeRenderer shape;

    public ParticleSystem() {
        shape = new ShapeRenderer();
    }

    public void spawnParticle(float x, float y, float z,
                              float velX, float velY, float velZ,
                              float s, float r, float g, float b, float a,
                              float lifetime) {
        if (count >= MAX_PARTICLES) return; // Ignore if full (or could do ring buffer)
        
        int i = count;
        px[i] = x; py[i] = y; pz[i] = z;
        vx[i] = velX; vy[i] = velY; vz[i] = velZ;
        size[i] = s;
        colorR[i] = r; colorG[i] = g; colorB[i] = b; colorA[i] = a;
        life[i] = lifetime;
        maxLife[i] = lifetime;
        count++;
    }

    public void update(float dt) {
        for (int i = 0; i < count; i++) {
            life[i] -= dt;
            if (life[i] <= 0) {
                // Swap with last active particle to remove
                count--;
                px[i] = px[count]; py[i] = py[count]; pz[i] = pz[count];
                vx[i] = vx[count]; vy[i] = vy[count]; vz[i] = vz[count];
                size[i] = size[count];
                colorR[i] = colorR[count]; colorG[i] = colorG[count];
                colorB[i] = colorB[count]; colorA[i] = colorA[count];
                life[i] = life[count]; maxLife[i] = maxLife[count];
                i--; // Re-evaluate swapped particle
                continue;
            }
            
            px[i] += vx[i] * dt;
            py[i] += vy[i] * dt;
            pz[i] += vz[i] * dt;
            
            // simple drag / gravity could be added here
            vx[i] *= 0.95f;
            vz[i] *= 0.95f;
            vy[i] -= 15f * dt; // gravity
        }
    }

    public void render(Camera3D cam) {
        if (count == 0) return;
        
        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();
        Matrix4 ortho = new Matrix4().setToOrtho2D(0, 0, W, H);
        shape.setProjectionMatrix(ortho);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < count; i++) {
            proj.set(px[i], py[i], pz[i]);
            cam.cam.project(proj);
            
            if (proj.z <= 0f || proj.z >= 1f) continue;
            if (proj.x < -50 || proj.x > W + 50) continue;
            if (proj.y < -50 || proj.y > H + 50) continue;
            
            float dist = cam.cam.position.dst(px[i], py[i], pz[i]);
            float sz = Math.max(1f, (size[i] * 100f) / Math.max(dist, 1f));
            
            float alpha = colorA[i] * (life[i] / maxLife[i]);
            shape.setColor(colorR[i], colorG[i], colorB[i], alpha);
            shape.rect(proj.x - sz * 0.5f, proj.y - sz * 0.5f, sz, sz);
        }
        
        shape.end();
    }

    public void dispose() {
        shape.dispose();
    }
}
