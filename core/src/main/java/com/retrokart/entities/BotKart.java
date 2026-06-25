package com.retrokart.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.retrokart.physics.TerrainSampler3D;
import com.retrokart.rendering.Camera3D;

/**
 * BotKart – waypoint-following bot with angle-based 22-frame sprite selection.
 */
public class BotKart extends Entity {

    // ── Sprite ────────────────────────────────────────────────────────
    private static final int   NUM_FRAMES    = 22;

    // ── Physics ───────────────────────────────────────────────────────
    private static final float MAX_SPEED     = 45f;   // Slower than player so they can catch up
    private static final float CORNER_SPEED  = 15f;   // slow down on tight bends
    private static final float STEER_SPEED   = 4.5f;  // rad/sec turning rate
    private static final float REACH_RADIUS  = 35f;   // tighter – don't skip corners

    // ── Waypoints (original recorded line) ───────────────────────────
    private static final float[][] WAYPOINTS = {
        { -882.3f,  -1628.4f },   // 0  spawn
        { -960.0f,  -1615.0f },   // 1  ← new: stay on road after start
        { -1040.0f, -1598.0f },   // 2  ← new
        { -1100.0f, -1575.0f },   // 3  ← new: past first item box
        { -1160.0f, -1530.0f },   // 4  ← new
        { -1210.0f, -1470.0f },   // 5  ← new
        { -1235.8f, -1425.1f },   // 6  (was 1)
        { -1206.5f, -1365.2f },   // 7  (was 2)
        { -1283.9f, -1248.8f },   // 8  (was 3)
        { -1196.8f, -1176.9f },   // 9  (was 4)
        { -1148.3f, -1066.6f },   // 10 (was 5)
        { -1213.8f,  -968.2f },   // 11 (was 6)
        { -1336.6f,  -977.4f },   // 12 (was 7)
        { -1453.1f,  -940.1f },   // 13 (was 8)
        { -1513.7f,  -832.6f },   // 14 (was 9)
        { -1590.4f,  -738.1f },   // 15 (was 10)
        { -1708.6f,  -720.2f },   // 16 (was 11)
        { -1768.8f,  -824.5f },   // 17 (was 12)
        { -1849.5f,  -913.7f },   // 18 (was 13)
        { -1929.3f,  -939.9f },   // 19 (was 14)
        { -1945.4f,  -949.7f },   // 20 finish
    };

    // ── State ─────────────────────────────────────────────────────────
    private final TerrainSampler3D terrainSampler;
    private float smoothWorldY = 0f;
    private int   waypointIdx  = 1;
    private float speed        = 0f;
    private boolean finished   = false;

    public BotKart(Texture sheet, float startX, float startZ,
                   TerrainSampler3D sampler) {
        super(sheet, 32, 32, 1, startX, startZ);
        setTag("BotKart");
        this.terrainSampler = sampler;
        this.spriteWorldSize = 52f;
        this.angle = -MathUtils.PI / 2f;
    }

    /** Unit 2: abstract method implementation from GameEntity. */
    @Override
    public String describe() {
        return String.format("BotKart wp=%d spd=%.1f finished=%b",
            waypointIdx, speed, finished);
    }

    @Override
    public void update(float dt) {
        // Terrain height sync
        float th = terrainSampler.getHeight(x, y);
        smoothWorldY = MathUtils.lerp(smoothWorldY, th, 12f * dt);
        worldY = smoothWorldY;

        if (finished || waypointIdx >= WAYPOINTS.length) return;

        // Current target
        float tx = WAYPOINTS[waypointIdx][0];
        float tz = WAYPOINTS[waypointIdx][1];
        float dx = tx - x;
        float dz = tz - y;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        // Advance waypoint when close enough
        if (dist < REACH_RADIUS) {
            waypointIdx++;
            if (waypointIdx >= WAYPOINTS.length) {
                finished = true;
                speed = 0f;
                return;
            }
            tx = WAYPOINTS[waypointIdx][0];
            tz = WAYPOINTS[waypointIdx][1];
            dx = tx - x;
            dz = tz - y;
        }

        // Desired heading
        float desiredAngle = MathUtils.atan2(dx, dz);

        // Shortest-path angle difference
        float angleDiff = desiredAngle - angle;
        while (angleDiff >  MathUtils.PI) angleDiff -= MathUtils.PI2;
        while (angleDiff < -MathUtils.PI) angleDiff += MathUtils.PI2;

        // Steer
        float steer = MathUtils.clamp(angleDiff, -STEER_SPEED * dt, STEER_SPEED * dt);
        angle += steer;

        // Slow down proportionally to how sharp the turn is
        float turnSharpness = Math.abs(angleDiff) / MathUtils.PI;  // 0=straight, 1=U-turn
        float targetSpeed = MathUtils.lerp(MAX_SPEED, CORNER_SPEED, Math.min(turnSharpness * 3f, 1f));
        speed = MathUtils.lerp(speed, targetSpeed, 4f * dt);

        // Move
        x += MathUtils.sin(angle) * speed * dt;
        y += MathUtils.cos(angle) * speed * dt;
        velocity.set(MathUtils.sin(angle) * speed, MathUtils.cos(angle) * speed);
    }

    @Override
    protected TextureRegion selectFrame(Camera3D cam) {
        float toCamX = cam.cam.position.x - x;
        float toCamZ = cam.cam.position.z - y;
        float viewAngle = MathUtils.atan2(toCamX, toCamZ);

        float relAngle = viewAngle - angle;
        relAngle = ((relAngle % MathUtils.PI2) + MathUtils.PI2) % MathUtils.PI2;

        int frameIndex = Math.round(relAngle / (MathUtils.PI2 / NUM_FRAMES)) % NUM_FRAMES;
        return frames[0][frameIndex];
    }
}
