package com.retrokart.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.retrokart.physics.TerrainSampler3D;
import com.retrokart.physics.TrackSurface;
import com.retrokart.rendering.Camera3D;
import com.retrokart.interfaces.Driveable;

/**
 * Kart – physics + steering-frame animation.
 * Implements Driveable (Unit 3: interface implementation).
 */
public class Kart extends Entity implements Driveable {

    public static final float ACCEL_ROAD     = 120f;
    public static final float ACCEL_GRASS    =  45f;
    public static final float MAX_SPEED_ROAD =  80f;
    public static final float MAX_SPEED_GRASS=  30f;
    public static final float DRAG_ROAD      =  0.96f;
    public static final float DRAG_GRASS     =  0.88f;
    public static final float BRAKE_FORCE    = 200f;
    public static final float REVERSE_ACCEL  =  60f;
    public static final float REVERSE_MAX    =  20f;

    public static final float STEER_RATE         = 2.6f;
    public static final float STEER_SPEED_FACTOR = 0.015f;
    public static final float STEER_RETURN        = 6f;

    public static final float DRIFT_SLIP           = 0.55f;
    public static final float DRIFT_BOOST          = 1.25f;
    public static final float DRIFT_SPIN           = 2.0f; // Lowered from 3.8f
    public static final float DRIFT_BOOST_DURATION = 0.5f;

    private float   speed       = 0f;
    private float   steerAngle  = 0f;
    public  float   steerSmooth = 0f;
    private boolean drifting    = false;
    private boolean driftLeft   = false;
    private float   driftTimer  = 0f;
    private float   boostTimer  = 0f;
    private TrackSurface surface = TrackSurface.ROAD;

    public boolean wheelSpin = false;
    public float   skidAlpha = 0f;
    public Color   kartColor;

    private final boolean isPlayer;
    private final boolean useSteeringSheet;
    private final TerrainSampler3D terrainSampler;

    // Smooth worldY
    private float smoothWorldY = 0f;

    public Kart(Texture spriteSheet, int frameW, int frameH, int rows,
                float startX, float startZ,
                Color color,
                TerrainSampler3D sampler,
                boolean isPlayer,
                boolean useSteeringSheet) {
        super(spriteSheet, frameW, frameH, rows, startX, startZ);
        setTag(isPlayer ? "PlayerKart" : "AIKart");
        this.kartColor        = color;
        this.terrainSampler   = sampler;
        this.isPlayer         = isPlayer;
        this.useSteeringSheet = useSteeringSheet;
        this.spriteWorldSize  = 32f;
    }

    // ── Driveable interface implementation (Unit 3) ───────────────────

    @Override public float   getSpeed()      { return speed; }
    @Override public float   getSteerAngle() { return steerAngle; }
    @Override public boolean isDrifting()    { return drifting; }

    @Override
    public void accelerate(float force) {
        float accelForce = surface == TrackSurface.ROAD ? ACCEL_ROAD : ACCEL_GRASS;
        speed += accelForce * force;
    }

    @Override
    public void brake(float force) {
        if (speed > 1f) speed -= BRAKE_FORCE * force;
        else            speed -= REVERSE_ACCEL * force;
    }

    /** Unit 2: abstract method implementation. */
    @Override
    public String describe() {
        return String.format("Kart[%s] spd=%.1f drift=%b surface=%s",
            getTag(), speed, drifting, surface);
    }

    @Override
    public void update(float dt) {
        if (isPlayer) readInput(dt);

        float prevX = x;
        float prevY = y;

        integrate(dt);

        TrackSurface newSurface = terrainSampler.getSurface(x, y);

        // Only block the kart if there is TRULY no ground (water, void, outside model).
        // On grass/sand/shoulders the kart slows down naturally via ACCEL_GRASS / DRAG_GRASS.
        boolean noGround = (newSurface == TrackSurface.OFFTRACK)
                        && !terrainSampler.hasGround(x, y);

        if (noGround) {
            // Hard boundary: revert position, bleed speed
            x = prevX;
            y = prevY;
            speed *= 0.7f;
            surface = terrainSampler.getSurface(x, y);
        } else {
            surface = newSurface;
        }

        applyDrag(dt);
        applyBoost(dt);      // boost first, then clamp (so boost can exceed normal max)
        if (boostTimer <= 0f) clampSpeed();  // only clamp when not boosting

        // Update terrain height
        float th = terrainSampler.getHeight(x, y);
        smoothWorldY = MathUtils.lerp(smoothWorldY, th, 12f * dt);
        worldY = smoothWorldY;

        // Steer smooth for animation
        float targetSmooth = steerAngle / STEER_RATE;
        steerSmooth = MathUtils.lerp(steerSmooth, targetSmooth, 12f * dt);
        steerSmooth = MathUtils.clamp(steerSmooth, -1f, 1f);
    }

    private void readInput(float dt) {
        boolean accel    = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean brake    = Gdx.input.isKeyPressed(Input.Keys.DOWN);
        boolean left     = Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right    = Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean driftKey = Gdx.input.isKeyPressed(Input.Keys.Z);

        float accelForce = surface == TrackSurface.ROAD ? ACCEL_ROAD : ACCEL_GRASS;
        if (accel) speed += accelForce * dt;
        if (brake) {
            if (speed > 1f) speed -= BRAKE_FORCE * dt;
            else            speed -= REVERSE_ACCEL * dt;
        }
        if (!accel && !brake) speed *= 0.99f;

        float steerMax = STEER_RATE * (1f - Math.min(Math.abs(speed) * STEER_SPEED_FACTOR, 0.6f));
        if (left)       steerAngle = -steerMax;
        else if (right) steerAngle =  steerMax;
        else            steerAngle  = MathUtils.lerp(steerAngle, 0f, STEER_RETURN * dt);

        boolean canDrift = Math.abs(speed) > 20f;
        if (driftKey && canDrift) {
            if (!drifting) { drifting=true; driftLeft=(steerAngle>0); driftTimer=0f; }
            driftTimer += dt;
        } else if (drifting) {
            if (driftTimer > 0.8f) boostTimer = DRIFT_BOOST_DURATION;
            drifting = false;
        }

        wheelSpin = accel && speed < 10f;
        skidAlpha = drifting ? MathUtils.clamp(driftTimer * 2f, 0f, 1f) : 0f;
    }

    private void integrate(float dt) {
        float maxSpd = surface == TrackSurface.ROAD ? MAX_SPEED_ROAD : MAX_SPEED_GRASS;
        float turnDelta;
        if (drifting) {
            float bias = driftLeft ? 1f : -1f;
            turnDelta = (steerAngle * DRIFT_SPIN + bias * DRIFT_SPIN * 0.3f) * dt;
        } else {
            float speedFrac = Math.abs(speed) / (maxSpd + 0.001f);
            turnDelta = steerAngle * speedFrac * dt;
        }
        angle += turnDelta;

        float fwdX = MathUtils.sin(angle);
        float fwdZ = MathUtils.cos(angle);
        if (drifting) {
            velocity.x = MathUtils.lerp(velocity.x, fwdX*speed, DRIFT_SLIP*dt*10f);
            velocity.y = MathUtils.lerp(velocity.y, fwdZ*speed, DRIFT_SLIP*dt*10f);
        } else {
            velocity.x = fwdX * speed;
            velocity.y = fwdZ * speed;
        }
        x += velocity.x * dt;
        y += velocity.y * dt;
    }

    private void applyDrag(float dt) {
        float drag = surface == TrackSurface.ROAD ? DRAG_ROAD : DRAG_GRASS;
        speed *= (float) Math.pow(drag, dt * 60f);
    }

    private void clampSpeed() {
        float maxSpd = surface == TrackSurface.ROAD ? MAX_SPEED_ROAD : MAX_SPEED_GRASS;
        speed = MathUtils.clamp(speed, -REVERSE_MAX, maxSpd);
    }

    private void applyBoost(float dt) {
        if (boostTimer > 0f) {
            boostTimer -= dt;
            float boostMax = MAX_SPEED_ROAD * 1.25f;
            // Push speed toward boost max (don't multiply - that causes runaway)
            if (speed < boostMax) {
                speed = Math.min(speed + ACCEL_ROAD * dt * 2f, boostMax);
            }
            speed = Math.min(speed, boostMax); // hard cap
        }
    }

    public boolean resolveCollision(Kart other) {
        if (!overlaps(other)) return false;
        float dx=x-other.x, dy=y-other.y;
        float dist=(float)Math.sqrt(dx*dx+dy*dy);
        if (dist<0.001f){dx=1;dist=1;}
        float nx=dx/dist, ny=dy/dist;
        float overlap=(collisionRadius()+other.collisionRadius())-dist;
        x+=nx*overlap*.5f; y+=ny*overlap*.5f;
        other.x-=nx*overlap*.5f; other.y-=ny*overlap*.5f;
        float rvx=velocity.x-other.velocity.x, rvy=velocity.y-other.velocity.y;
        float impulse=(rvx*nx+rvy*ny)*.6f;
        speed-=impulse; other.speed+=impulse;
        return true;
    }

    public boolean resolveCollision(BotKart other) {
        if (!overlaps(other)) return false;
        float dx=x-other.x, dy=y-other.y;
        float dist=(float)Math.sqrt(dx*dx+dy*dy);
        if (dist<0.001f){dx=1;dist=1;}
        float nx=dx/dist, ny=dy/dist;
        
        // Very tight collision radar (must be practically clipping into the sprite)
        float overlap = (collisionRadius()*0.35f + other.collisionRadius()*0.35f) - dist;
        if (overlap <= 0) return false;
        
        // Push player out of the bot
        x += nx * overlap; 
        y += ny * overlap;
        
        // Arcade bump: instead of complex impulse math that can invert, just kill some speed
        speed *= 0.6f; 
        
        return true;
    }

    @Override
    protected TextureRegion selectFrame(Camera3D cam) {
        if (!useSteeringSheet) return super.selectFrame(cam);
        int COLS = frames[0].length;
        float normalised = (steerSmooth + 1f) * 0.5f;
        int col = MathUtils.clamp(Math.round(normalised * (COLS-1)), 0, COLS-1);
        return frames[0][col];
    }

    public boolean      isBoosting() { return boostTimer > 0f; }
    public TrackSurface getSurface() { return surface;  }

    /** Feature 7 - called when kart collects a boost item box. */
    public void applyPickupBoost() {
        boostTimer = 2.0f;   // 2-second boost (longer than drift boost)
    }

    public void setSteerAngle(float a) { this.steerAngle = a; }
    public void setSpeed(float s)      { this.speed = s; }
    public void updateSteerSmooth(float dt) {
        float t = steerAngle / STEER_RATE;
        steerSmooth = MathUtils.lerp(steerSmooth, t, 12f * dt);
        steerSmooth = MathUtils.clamp(steerSmooth, -1f, 1f);
    }
}
