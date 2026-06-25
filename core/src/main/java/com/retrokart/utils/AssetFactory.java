package com.retrokart.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.utils.UBJsonReader;

import java.util.ArrayList;
import java.util.List;

public class AssetFactory {

    private static AssetFactory INSTANCE;
    public static void init()    { INSTANCE = new AssetFactory(); }
    public static void dispose() { if (INSTANCE != null) INSTANCE.disposeAll(); }

    public static Model   getTrackModel()      { return INSTANCE.trackModel;      }
    public static Texture getPlayerKartSheet() { return INSTANCE.playerKartSheet; }
    public static Texture getCpuKartSheet()    { return INSTANCE.cpuKartSheet;    }
    public static Texture getItemBoxTexture()  { return INSTANCE.itemBoxTexture;  }
    public static Texture getBowserSheet()     { return INSTANCE.bowserSheet;     }

    private Model   trackModel;
    private Texture playerKartSheet;
    private Texture cpuKartSheet;
    private Texture itemBoxTexture;
    private Texture bowserSheet;   // 22-frame rotation strip (704×32)
    private final List<Texture> managedTex = new ArrayList<>();

    public static final int   PLAYER_FRAME_W      = 32;
    public static final int   PLAYER_FRAME_H      = 32;
    public static final int   PLAYER_STEER_FRAMES = 7;
    public static final int   PLAYER_ROWS         = 1;
    public static final int   KART_FRAME_W = 32;
    public static final int   KART_FRAME_H = 32;
    public static final int   KART_DIRS    = 8;
    public static final int   KART_ROWS    = 1;
    public static final float MODEL_SCALE  = 0.05f;

    private AssetFactory() {
        loadTrackModel();
        loadPlayerKartSheet();
        cpuKartSheet   = buildCpuKartSheet(new Color(1f, 0.25f, 0.1f, 1f));
        itemBoxTexture = buildItemBox();
        bowserSheet    = loadBowserSheet();
    }

    // ── Track model ───────────────────────────────────────────────────
    private void loadTrackModel() {
        // G3dModelLoader resolves textures relative to the model file's folder.
        // Our model is at  assets/models/track.g3db
        // Texture ref in g3db: "Sprite-0001-export-export.png"
        // → LibGDX will look for assets/models/Sprite-0001-export-export.png  ✓
        G3dModelLoader loader = new G3dModelLoader(new UBJsonReader());
        trackModel = loader.loadModel(Gdx.files.internal("models/track.g3db"));

        Gdx.app.log("AssetFactory", "Track model loaded — "
                + trackModel.meshes.size    + " meshes, "
                + trackModel.materials.size + " materials");

        // Apply Nearest filter so the palette texture stays crisp (pixel-art style)
        for (Material mat : trackModel.materials) {
            TextureAttribute ta = (TextureAttribute)
                    mat.get(TextureAttribute.Diffuse);
            if (ta != null && ta.textureDescription.texture != null) {
                ta.textureDescription.texture.setFilter(
                        TextureFilter.Nearest, TextureFilter.Nearest);
                Gdx.app.log("AssetFactory",
                        "  Material '" + mat.id + "' texture loaded OK");
            }
        }
    }

    // ── Player kart ───────────────────────────────────────────────────
    private void loadPlayerKartSheet() {
        playerKartSheet = new Texture(
                Gdx.files.internal("textures/mario_kart_sheet.png"));
        playerKartSheet.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        managedTex.add(playerKartSheet);
    }

    // ── CPU kart (procedural coloured shape) ─────────────────────────
    private Texture buildCpuKartSheet(Color c) {
        int W = KART_FRAME_W * KART_DIRS, H = KART_FRAME_H;
        Pixmap pm = new Pixmap(W, H, Pixmap.Format.RGBA8888);
        pm.setColor(0,0,0,0); pm.fill();
        float[] angles = {0,45,90,135,180,225,270,315};
        for (int col = 0; col < KART_DIRS; col++)
            drawKartFrame(pm, col * KART_FRAME_W, 0,
                          KART_FRAME_W, KART_FRAME_H,
                          (float) Math.toRadians(angles[col]), c);
        Texture t = new Texture(pm);
        t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        pm.dispose(); managedTex.add(t); return t;
    }

    private void drawKartFrame(Pixmap pm, int ox, int oy, int fw, int fh,
                               float angle, Color c) {
        float cx = ox + fw * .5f, cy = oy + fh * .5f, r = fw * .38f;
        for (int py = oy; py < oy + fh; py++)
        for (int px = ox; px < ox + fw; px++) {
            float lx = px - cx, ly = py - cy;
            float ca = (float) Math.cos(-angle), sa = (float) Math.sin(-angle);
            float kx = lx*ca - ly*sa, ky = lx*sa + ly*ca;
            float hw = Math.max(.1f, Math.min(r * (0.55f + 0.35f * (ky / (r*1.2f+.01f))), r));
            float ex = kx/hw, ey = ky/(r*1.1f);
            if (ex*ex + ey*ey <= 1f) {
                float s = 1f - (ex*ex + ey*ey) * .35f;
                pm.setColor(c.r*s, c.g*s, c.b*s, 1f);
                pm.drawPixel(px, py);
            }
            float[][] w = {{r*.65f,-r*.7f},{-r*.65f,-r*.7f},{r*.65f,r*.6f},{-r*.65f,r*.6f}};
            for (float[] wh : w) {
                float wx = wh[0]*ca + wh[1]*sa + cx - ox;
                float wy = -wh[0]*sa + wh[1]*ca + cy - oy;
                float ddx = px-ox-wx, ddy = py-oy-wy;
                if (ddx*ddx + ddy*ddy < 5) { pm.setColor(.1f,.1f,.1f,1f); pm.drawPixel(px,py); }
            }
        }
    }

    // ── Item box ──────────────────────────────────────────────────────
    private Texture buildItemBox() {
        Pixmap pm = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
        pm.setColor(0,0,0,0); pm.fill();
        for (int i = 0; i < 32; i++) {
            float t = i / 32f; pm.setColor(t, 1-t, .5f, 1f);
            pm.drawPixel(i,0); pm.drawPixel(i,31);
            pm.drawPixel(0,i); pm.drawPixel(31,i);
        }
        pm.setColor(1f,1f,0f,1f);
        int[][] q = {{11,8},{12,8},{13,8},{14,8},{15,8},{16,8},{17,8},{18,8},
                     {9,9},{10,9},{19,9},{20,9},{19,10},{20,10},{18,11},{19,11},
                     {17,12},{18,12},{15,13},{16,13},{17,13},{15,14},{16,14},
                     {15,15},{16,15},{15,16},{16,16},{15,17},{16,17},
                     {15,20},{16,20},{15,21},{16,21},{15,22},{16,22}};
        for (int[] p : q) pm.drawPixel(p[0], p[1]);
        Texture t = new Texture(pm); pm.dispose(); managedTex.add(t); return t;
    }

    // ── Bowser bot sprite strip (22 frames × 32px wide = 704×32) ─────
    private Texture loadBowserSheet() {
        Texture t = new Texture(Gdx.files.internal("textures/bowser_sheet.png"));
        t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        managedTex.add(t);
        return t;
    }

    private void disposeAll() {
        if (trackModel != null) trackModel.dispose();
        for (Texture t : managedTex) t.dispose();
    }
}
