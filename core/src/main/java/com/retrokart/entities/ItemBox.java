package com.retrokart.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.retrokart.interfaces.Collectable;

/**
 * ItemBox – spinning speed-boost pickup.
 *
 * Syllabus coverage (TCS-408):
 *  Unit 3 – Implements Collectable interface (interface implementation).
 *            Interface reference: ItemBox is stored as Collectable in RaceScreen.
 *  Unit 2 – Inheritance from Entity (which extends GameEntity).
 *            describe() overrides abstract method from GameEntity.
 */
public class ItemBox extends Entity implements Collectable {

    private static final float RESPAWN_TIME = 5.0f;

    private float   respawnTimer = 0f;
    private boolean collected    = false;
    private final float originX, originY;

    public ItemBox(Texture tex, float x, float y) {
        super(tex, x, y);
        setTag("ItemBox");
        this.originX = x;
        this.originY = y;
        this.spriteWorldSize = 24f;
    }

    public ItemBox(Texture tex) {
        this(tex, 0f, 0f);
    }

    @Override
    public void update(float dt) {
        if (collected) {
            respawnTimer -= dt;
            if (respawnTimer <= 0f) {
                collected    = false;
                active       = true;
                x            = originX;
                y            = originY;
            }
            return;
        }
        angle += dt * MathUtils.PI2 * 0.8f;
    }

    @Override
    public String describe() {
        return String.format("ItemBox @ (%.1f, %.1f) [%s]",
            x, y, collected ? "USED" : "READY");
    }

    @Override
    public void collect() {
        if (collected) return;
        collected    = true;
        active       = false;
        respawnTimer = RESPAWN_TIME;
    }

    @Override
    public boolean isCollected() { return collected; }

    @Override
    public String getItemType() { return "BoostBox"; }
}
