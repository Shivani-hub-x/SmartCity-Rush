package com.retrokart.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.retrokart.rendering.Camera3D;

/**
 * Entity – base class for all billboard sprites in the 3D world.
 *
 * Extends GameEntity to participate in the full OOP hierarchy (Unit 2):
 *   GameEntity (abstract) → Entity (concrete) → Kart / BotKart / ItemBox
 *
 * x, y  = horizontal plane position  (y maps to 3D Z)
 * worldY = vertical position (terrain height, updated each frame)
 * angle  = yaw in radians
 */
public class Entity extends GameEntity {

    public float x, y;
    public float worldY = 0f;
    public float angle  = 0f;
    public boolean active = true;

    protected TextureRegion[][] frames;
    protected int   animRow        = 0;
    public float spriteWorldSize = 48f;

    public float depth = Float.MAX_VALUE;
    public final Vector2 velocity = new Vector2();

    private final Vector3 projVec = new Vector3();

    public Entity(Texture tex, float x, float y) {
        super("Entity");
        this.x = x; this.y = y;
        frames = new TextureRegion[1][1];
        frames[0][0] = new TextureRegion(tex);
    }

    public Entity(Texture sheet, int frameW, int frameH, int rows, float x, float y) {
        super("Entity");
        this.x = x; this.y = y;
        frames = TextureRegion.split(sheet, frameW, frameH);
    }

    @Override
    public void update(float dt) {}

    /** Describe this entity for debug / logging (Unit 2: abstract method impl). */
    @Override
    public String describe() {
        return String.format("%s @ (%.1f, %.1f) h=%.1f", getTag(), x, y, worldY);
    }

    public boolean draw(SpriteBatch batch, Camera3D cam,
                        float screenW, float screenH) {
        projVec.set(x, worldY + spriteWorldSize * 0.01f, y);
        float dist = cam.cam.position.dst(projVec);
        depth = dist;

        cam.cam.project(projVec);
        if (projVec.z <= 0f || projVec.z >= 1f) return false;

        float sx = projVec.x;
        float sy = projVec.y;

        float scale = (spriteWorldSize * 1.2f) / Math.max(dist, 0.1f);
        if (scale < 0.1f) return false;

        TextureRegion region = selectFrame(cam);
        float drawW = region.getRegionWidth()  * scale;
        float drawH = region.getRegionHeight() * scale;

        if (sx + drawW < 0 || sx - drawW > screenW) return false;
        if (sy + drawH < 0 || sy - drawH > screenH) return false;

        batch.draw(region, sx - drawW * 0.5f, sy - drawH * 0.5f, drawW, drawH);
        return true;
    }

    protected TextureRegion selectFrame(Camera3D cam) {
        int cols = frames[0].length;
        if (cols == 1) return frames[Math.min(animRow, frames.length-1)][0];

        float toCamX = cam.cam.position.x - x;
        float toCamZ = cam.cam.position.z - y;
        float viewAngle = MathUtils.atan2(toCamX, toCamZ);

        float relAngle = viewAngle - angle;
        while (relAngle <  0)             relAngle += MathUtils.PI2;
        while (relAngle >= MathUtils.PI2) relAngle -= MathUtils.PI2;

        int dirIndex = (int)((relAngle + MathUtils.PI2/16f) / (MathUtils.PI2/8f)) % 8;
        int col = Math.min(dirIndex, cols-1);
        return frames[Math.min(animRow, frames.length-1)][col];
    }

    public float collisionRadius() { return spriteWorldSize * 0.4f; }

    public boolean overlaps(Entity other) {
        float dx = x - other.x, dy = y - other.y;
        float r  = collisionRadius() + other.collisionRadius();
        return dx*dx + dy*dy < r*r;
    }
}
