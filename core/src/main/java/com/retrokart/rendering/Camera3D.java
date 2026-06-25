package com.retrokart.rendering;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/**
 * Camera3D
 *
 * Wraps LibGDX PerspectiveCamera and positions it behind + above the kart.
 *
 * Coordinate convention (LibGDX 3D):
 *   Y = up,  X = right,  Z = toward viewer (right-hand)
 *
 * Kart physics use (kart.x, kart.y) as the horizontal plane → 3D (kart.x, worldY, kart.y)
 * kart.angle rotates around the Y axis.
 */
public class Camera3D {

    public final PerspectiveCamera cam;

    /** Current yaw angle, kept in sync with the kart. */
    public float angle = 0f;

    // Tuning
    private static final float DIST_BACK  = 12f;   // units behind kart
    private static final float CAM_HEIGHT  = 6f;   // units above kart
    private static final float LOOK_AHEAD  = 4f;   // look-ahead offset
    private static final float POS_SPRING  = 10f;  // camera smoothing

    private final Vector3 desiredPos    = new Vector3();
    private final Vector3 desiredTarget = new Vector3();

    // Camera Shake
    private float shakeDuration = 0f;
    private float shakeIntensity = 0f;

    public Camera3D(float viewW, float viewH) {
        cam = new PerspectiveCamera(65f, viewW, viewH);
        cam.near = 0.3f;
        cam.far  = 3000f;
        cam.up.set(0, 1, 0);
    }

    /**
     * Smooth-follow the kart every frame.
     *
     * @param kx      kart world X
     * @param ky      kart world Y  (terrain height)
     * @param kz      kart world Z  (= kart.y in 2-D physics)
     * @param kAngle  kart yaw (radians)
     * @param dt      delta time
     */
    public void follow(float kx, float ky, float kz, float kAngle, float dt) {
        angle = kAngle;

        float sinA = MathUtils.sin(kAngle);
        float cosA = MathUtils.cos(kAngle);

        desiredPos.set(
                kx  - sinA * DIST_BACK,
                ky  + CAM_HEIGHT,
                kz  - cosA * DIST_BACK);

        desiredTarget.set(
                kx  + sinA * LOOK_AHEAD,
                ky  + 1.5f,
                kz  + cosA * LOOK_AHEAD);

        cam.position.lerp(desiredPos, POS_SPRING * dt);
        
        float px = cam.position.x;
        float py = cam.position.y;
        float pz = cam.position.z;
        
        cam.lookAt(desiredTarget);
        cam.up.set(0, 1, 0);

        if (shakeDuration > 0) {
            shakeDuration -= dt;
            float offsetX = MathUtils.random(-1f, 1f) * shakeIntensity;
            float offsetY = MathUtils.random(-1f, 1f) * shakeIntensity;
            float offsetZ = MathUtils.random(-1f, 1f) * shakeIntensity;
            cam.position.add(offsetX, offsetY, offsetZ);
            cam.direction.add(offsetX * 0.1f, offsetY * 0.1f, offsetZ * 0.1f);
        }

        cam.update();
        
        // Restore actual position so we don't feed shake noise back into the lerp
        cam.position.set(px, py, pz);
    }

    public void addShake(float intensity, float duration) {
        this.shakeIntensity = intensity;
        this.shakeDuration = Math.max(this.shakeDuration, duration);
    }

    /** Hard-snap to starting position (no lerp). */
    public void snapTo(float kx, float ky, float kz, float kAngle) {
        angle = kAngle;
        float sinA = MathUtils.sin(kAngle);
        float cosA = MathUtils.cos(kAngle);

        cam.position.set(
                kx - sinA * DIST_BACK,
                ky + CAM_HEIGHT,
                kz - cosA * DIST_BACK);
        cam.lookAt(kx, ky, kz);
        cam.up.set(0, 1, 0);
        cam.update();
    }

    /**
     * Project a 3D world point to screen coordinates.
     * z result: 0=near plane, 1=far plane.
     * Returns the input vector modified in-place.
     */
    public Vector3 project(Vector3 world) {
        cam.project(world);
        return world;
    }
}
