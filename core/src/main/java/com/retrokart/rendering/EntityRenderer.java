package com.retrokart.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.retrokart.entities.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * EntityRenderer – Z-sorts and draws all billboard entities using Camera3D.
 */
public class EntityRenderer {

    private final List<Entity> visible = new ArrayList<>(64);
    private static final Comparator<Entity> DEPTH_DESC =
            (a, b) -> Float.compare(b.depth, a.depth);

    public void render(SpriteBatch batch, Camera3D cam, List<Entity> entities) {
        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();

        visible.clear();
        for (Entity e : entities) {
            if (e.active) visible.add(e);
        }

        visible.sort(DEPTH_DESC);

        for (Entity e : visible) {
            e.draw(batch, cam, W, H);
        }
    }
}
