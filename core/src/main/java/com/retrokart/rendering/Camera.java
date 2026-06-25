package com.retrokart.rendering;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Camera – sits BEHIND_DIST units behind the kart.
 * Kart is always visible in lower-centre of screen.
 */
public class Camera {

    public float x, y, angle, height, fov, horizon;
    public float fogStart   = 800f;
    public float fogEnd     = 1800f;
    public float trackScale = 512f;
    public final Vector2 facing = new Vector2();

    // ── Tuning ────────────────────────────────────────────────────────
    private static final float BEHIND_DIST   = 48f;   // world units behind kart
    private static final float HEIGHT_NORMAL = 64f;
    private static final float HEIGHT_DRIFT  = 76f;
    private static final float HEIGHT_SPRING = 6f;

    /** Scale multiplier applied to sprite size in worldToScreen. */
    private static final float SPRITE_SCALE  = 2.5f;

    public Camera() {
        height  = HEIGHT_NORMAL;
        fov     = 1.05f;
        horizon = 0.52f;
        updateFacing();
    }

    public void follow(float kartX, float kartY, float kartAngle,
                       boolean drifting, float dt) {
        float fwdX = MathUtils.sin(kartAngle);
        float fwdY = MathUtils.cos(kartAngle);
        x     = kartX - fwdX * BEHIND_DIST;
        y     = kartY - fwdY * BEHIND_DIST;
        angle = kartAngle;
        float targetH = drifting ? HEIGHT_DRIFT : HEIGHT_NORMAL;
        height += (targetH - height) * HEIGHT_SPRING * dt;
        updateFacing();
    }

    public void snapTo(float kx, float ky, float ka) {
        float fwdX = MathUtils.sin(ka);
        float fwdY = MathUtils.cos(ka);
        x = kx - fwdX * BEHIND_DIST;
        y = ky - fwdY * BEHIND_DIST;
        angle  = ka;
        height = HEIGHT_NORMAL;
        updateFacing();
    }

    /**
     * Project world position to screen.
     * Returns {screenX, screenY, scale, depth} or null if behind camera.
     */
    public float[] worldToScreen(float worldX, float worldY,
                                  float screenW, float screenH) {
        float dx = worldX - x;
        float dy = worldY - y;

        float sinA = MathUtils.sin(angle);
        float cosA = MathUtils.cos(angle);
        float localX =  dx * cosA - dy * sinA;
        float localY =  dx * sinA + dy * cosA;

        if (localY <= 0.01f) return null;

        float halfW      = screenW * 0.5f;
        float tanHalfFov = (float) Math.tan(fov * 0.5);

        float sx = halfW + (localX / (localY * tanHalfFov)) * halfW;

        // Match shader formula: p = height / localY
        float p         = height / localY;
        // horizonPx: in LibGDX screen coords, Y=0 is BOTTOM
        // horizon uniform is fraction from bottom, so horizonPx = horizon * screenH
        float horizonPx = horizon * screenH;
        // Floor pixels are BELOW the horizon → sy = horizonPx - p
        float sy        = horizonPx - p;

        float scale = p * SPRITE_SCALE;

        return new float[]{ sx, sy, scale, localY };
    }

    private void updateFacing() {
        facing.set(MathUtils.sin(angle), MathUtils.cos(angle));
    }
}
