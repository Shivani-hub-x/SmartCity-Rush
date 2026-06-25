package com.retrokart.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.retrokart.entities.Kart;

/**
 * HUD – heads-up display.
 *
 * draw()        – speed, timer, countdown, win/lose overlay, best-time record
 * drawDriftBar  – orange drift charge bar
 * drawMinimap   – Feature 3: top-right corner minimap
 */
public class HUD implements Disposable {

    private final BitmapFont    font;
    private final ShapeRenderer shape;
    private final GlyphLayout   layout   = new GlyphLayout();
    private final com.badlogic.gdx.math.Vector3 skidProj = new com.badlogic.gdx.math.Vector3();

    // Pre-allocated colours to avoid per-frame GC pressure
    private static final Color COL_COUNT_NUM = new Color(1f, 0.85f, 0.1f, 1f);
    private static final Color COL_COUNT_GO  = new Color(0.1f, 1f, 0.3f, 1f);
    private static final Color COL_TIMER_RED = new Color(1f, 0.2f, 0.2f, 1f);
    private static final Color COL_WIN       = new Color(0.2f, 1f, 0.2f, 1f);
    private static final Color COL_LOSE      = new Color(1f, 0.25f, 0.25f, 1f);

    public HUD() {
        font  = new BitmapFont();
        font.getData().setScale(2f);
        shape = new ShapeRenderer();
    }

    /**
     * Main HUD draw call.
     *
     * @param countdownDone  true once the 3-2-1 has finished
     * @param countdownTimer seconds remaining in countdown (3 → 0)
     * @param bestTime       stored best completion time (0 = no record)
     * @param newBestTime    true if the player just set a new record this run
     */
    public void draw(SpriteBatch batch, Kart kart,
                     float raceTimer,
                     boolean raceFinished, boolean playerWon,
                     boolean countdownDone, float countdownTimer,
                     float bestTime, boolean newBestTime) {

        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();

        font.getData().setScale(2f);

        // ── Feature 1: Countdown overlay ─────────────────────────────
        if (!countdownDone) {
            font.getData().setScale(6f);
            int count = (int) Math.ceil(countdownTimer);
            String countStr = count > 0 ? String.valueOf(count) : "GO!";
            layout.setText(font, countStr);
            font.setColor(count > 0 ? COL_COUNT_NUM : COL_COUNT_GO);
            font.draw(batch, countStr,
                      (W - layout.width)  * 0.5f,
                      (H + layout.height) * 0.5f);
            font.getData().setScale(2f);

            // Show controls hint during countdown
            font.getData().setScale(1.2f);
            font.setColor(1f, 1f, 1f, 0.8f);
            String hint = "ARROWS = Drive    Z = Drift    R = Restart";
            layout.setText(font, hint);
            font.draw(batch, hint, (W - layout.width) * 0.5f, H - 20);
            font.getData().setScale(2f);
            return;  // skip rest of HUD during countdown
        }

        // ── Speed (bottom right) ──────────────────────────────────────
        String speedStr = String.format("%3.0f km/h", Math.abs(kart.getSpeed()) * 2.0f);
        layout.setText(font, speedStr);
        font.setColor(1f, 1f, 0.3f, 1f);
        font.draw(batch, speedStr, W - layout.width - 20, 50);

        // ── Timer (top centre) ────────────────────────────────────────
        if (!raceFinished) {
            String timeStr = String.format("TIME  %.1f", raceTimer);
            layout.setText(font, timeStr);
            font.setColor(raceTimer <= 10f ? COL_TIMER_RED : Color.WHITE);
            font.draw(batch, timeStr, (W - layout.width) * 0.5f, H - 20);
        }

        // ── Feature 5: Best time (top left) ──────────────────────────
        font.getData().setScale(1.3f);
        font.setColor(0.9f, 0.9f, 0.9f, 0.8f);
        if (bestTime > 0f) {
            font.draw(batch, String.format("BEST  %.1fs", bestTime), 20, H - 20);
        } else {
            font.draw(batch, "BEST  --", 20, H - 20);
        }

        // ── XYZ coordinates (top left, below BEST) ────────────────────
        font.getData().setScale(1.0f);
        font.setColor(0.7f, 1f, 0.7f, 0.85f);
        font.draw(batch, String.format("X %.1f  Y %.1f  Z %.1f",
                kart.x, kart.worldY, kart.y), 20, H - 42);
        font.getData().setScale(2f);

        // ── Controls hint (bottom left) ───────────────────────────────
        font.getData().setScale(1.1f);
        font.setColor(0.8f, 0.8f, 0.8f, 0.6f);
        font.draw(batch, "ARROWS=Drive  Z=Drift", 20, 30);
        font.getData().setScale(2f);

        // ── Boost indicator ───────────────────────────────────────────
        if (kart.isBoosting()) {
            font.setColor(1f, 0.4f, 0f, 1f);
            font.getData().setScale(2.5f);
            String boostStr = "BOOST!";
            layout.setText(font, boostStr);
            font.draw(batch, boostStr, (W - layout.width) * 0.5f, 110);
            font.getData().setScale(2f);
        }

        // ── Win / Lose overlay ────────────────────────────────────────
        if (raceFinished) {
            // Big result text
            font.getData().setScale(5f);
            String title = playerWon ? "YOU WIN!" : "TIME UP!";
            layout.setText(font, title);
            font.setColor(playerWon ? COL_WIN : COL_LOSE);
            font.draw(batch, title, (W - layout.width) * 0.5f, (H + layout.height) * 0.5f + 60);
            font.getData().setScale(2f);

            // Feature 5: completion time & best-time celebration
            if (playerWon) {
                float elapsed = 90f - raceTimer;
                String timeResult = String.format("Finished in  %.1f s", elapsed);
                layout.setText(font, timeResult);
                font.setColor(1f, 1f, 1f, 1f);
                font.draw(batch, timeResult, (W - layout.width) * 0.5f, (H) * 0.5f + 10);

                if (newBestTime) {
                    font.getData().setScale(2.2f);
                    font.setColor(1f, 0.9f, 0.1f, 1f);
                    String rec = "NEW BEST TIME!";
                    layout.setText(font, rec);
                    font.draw(batch, rec, (W - layout.width) * 0.5f, (H) * 0.5f - 30);
                    font.getData().setScale(2f);
                }
            }

            // Restart prompt
            font.getData().setScale(1.5f);
            font.setColor(0.9f, 0.9f, 0.9f, 0.85f);
            String restart = "Going to results...";
            layout.setText(font, restart);
            font.draw(batch, restart, (W - layout.width) * 0.5f, (H) * 0.5f - 80);
            font.getData().setScale(2f);
        }
    }

    /** Drift charge bar — shown while drifting. */
    public void drawDriftBar(Kart kart) {
        if (!kart.isDrifting()) return;
        float W = Gdx.graphics.getWidth();
        float barW = 200f, barH = 14f;
        float x = (W - barW) * 0.5f, y = 70f;
        Matrix4 ortho = new Matrix4().setToOrtho2D(0, 0, W, Gdx.graphics.getHeight());
        shape.setProjectionMatrix(ortho);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.1f, 0.1f, 0.1f, 0.7f);
        shape.rect(x - 2, y - 2, barW + 4, barH + 4);
        float fill = Math.min(kart.skidAlpha, 1f);
        shape.setColor(1f, 0.3f + fill * 0.5f, 0f, 1f);
        shape.rect(x, y, barW * fill, barH);
        shape.end();
    }

    /**
     * Feature 3 – minimap.
     *
     * Draws a small top-view rectangle in the top-right corner showing
     * the kart (blue dot), start (white dot) and finish (green dot).
     *
     * World bounds are estimated from the start & end coordinates so the
     * map always fits both points.
     */
    public void drawMinimap(Kart kart,
                            float startX, float startZ,
                            float endX,   float endZ) {

        float SW = Gdx.graphics.getWidth();
        float SH = Gdx.graphics.getHeight();

        // Minimap panel dimensions & position (top-right corner)
        float mapW = 130f, mapH = 100f;
        float mapX = SW - mapW - 14f;
        float mapY = SH - mapH - 14f;

        // World bounding box (generous padding so dots aren't on the edge)
        float padding = 200f;
        float worldMinX = Math.min(startX, endX) - padding;
        float worldMaxX = Math.max(startX, endX) + padding;
        float worldMinZ = Math.min(startZ, endZ) - padding;
        float worldMaxZ = Math.max(startZ, endZ) + padding;
        float worldW = worldMaxX - worldMinX;
        float worldH = worldMaxZ - worldMinZ;

        Matrix4 ortho = new Matrix4().setToOrtho2D(0, 0, SW, SH);
        shape.setProjectionMatrix(ortho);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        // Background panel
        shape.setColor(0f, 0f, 0f, 0.55f);
        shape.rect(mapX - 2, mapY - 2, mapW + 4, mapH + 4);
        shape.setColor(0.08f, 0.08f, 0.18f, 0.82f);
        shape.rect(mapX, mapY, mapW, mapH);

        // Helper: world → minimap screen coords
        // (world Z grows downward in libGDX XZ plane, flip it for top-down view)
        float sx = mapX + ((startX - worldMinX) / worldW) * mapW;
        float sz = mapY + (1f - (startZ - worldMinZ) / worldH) * mapH;
        float ex = mapX + ((endX - worldMinX) / worldW) * mapW;
        float ez = mapY + (1f - (endZ - worldMinZ) / worldH) * mapH;
        float kx = mapX + ((kart.x - worldMinX) / worldW) * mapW;
        float kz = mapY + (1f - (kart.y - worldMinZ) / worldH) * mapH;

        // Start dot (white)
        shape.setColor(1f, 1f, 1f, 0.9f);
        shape.circle(sx, sz, 4f);

        // Finish dot (green)
        shape.setColor(0.2f, 1f, 0.3f, 1f);
        shape.circle(ex, ez, 5f);

        // Kart dot (bright blue)
        shape.setColor(0.3f, 0.6f, 1f, 1f);
        shape.circle(kx, kz, 5f);

        shape.end();

        // Border
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.5f, 0.5f, 0.7f, 0.7f);
        shape.rect(mapX, mapY, mapW, mapH);
        shape.end();
    }

    /**
     * Feature 6 - Draw skid marks as small dark rectangles projected onto the screen.
     *
     * @param skidX  world X positions
     * @param skidZ  world Z positions
     * @param skidA  alpha values (fading over time)
     * @param count  number of active marks
     * @param cam    camera for projection
     */
    public void drawSkidMarks(float[] skidX, float[] skidZ, float[] skidA,
                               int count, com.retrokart.rendering.Camera3D cam,
                               float kartWorldY) {
        if (count == 0) return;
        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();
        Matrix4 ortho = new Matrix4().setToOrtho2D(0, 0, W, H);
        shape.setProjectionMatrix(ortho);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        com.badlogic.gdx.math.Vector3 proj = skidProj;  // reuse field, no GC
        for (int i = 0; i < count; i++) {
            float a = skidA[i];
            if (a <= 0.01f) continue;
            proj.set(skidX[i], kartWorldY, skidZ[i]);
            cam.cam.project(proj);
            if (proj.z <= 0f || proj.z >= 1f) continue;
            if (proj.x < -20 || proj.x > W + 20) continue;
            if (proj.y < -20 || proj.y > H + 20) continue;
            float dist = cam.cam.position.dst(skidX[i], kartWorldY, skidZ[i]);
            float sz   = Math.max(2f, 160f / Math.max(dist, 1f));
            shape.setColor(0.05f, 0.04f, 0.04f, a * 0.65f);
            shape.rect(proj.x - sz * 0.5f, proj.y - sz * 0.35f, sz, sz * 0.55f);
        }
        shape.end();
    }

    @Override
    public void dispose() {
        font.dispose();
        shape.dispose();
    }
}
