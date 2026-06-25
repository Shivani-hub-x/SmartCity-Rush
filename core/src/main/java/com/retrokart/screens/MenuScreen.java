package com.retrokart.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.retrokart.RetroKartGame;

/**
 * Feature 8 - Main menu screen shown before the race.
 * Press ENTER or SPACE to start racing.
 */
public class MenuScreen implements Screen {

    private final RetroKartGame game;
    private final SpriteBatch   batch;
    private final BitmapFont    font;
    private final GlyphLayout   layout = new GlyphLayout();
    private final ShapeRenderer shape;
    private final OrthographicCamera cam;

    // Simple animated pulse for the "Press ENTER" text
    private float pulseTimer = 0f;
    private final com.badlogic.gdx.math.Matrix4 shapeOrtho = new com.badlogic.gdx.math.Matrix4();

    // Scrolling wave animation
    private float waveOffset = 0f;

    public MenuScreen(RetroKartGame game) {
        this.game  = game;
        this.batch = game.batch;
        this.font  = new BitmapFont();
        this.shape = new ShapeRenderer();
        this.cam   = new OrthographicCamera();
        cam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void render(float dt) {
        pulseTimer += dt;
        waveOffset += dt * 60f;

        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();

        // Sky-blue background
        Gdx.gl.glClearColor(0.53f, 0.81f, 0.98f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw animated wave stripes at bottom (beach feel)
        shapeOrtho.setToOrtho2D(0, 0, W, H);
        shape.setProjectionMatrix(shapeOrtho);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        // Sand
        shape.setColor(0.96f, 0.87f, 0.60f, 1f);
        shape.rect(0, 0, W, H * 0.22f);

        // Animated wave bands
        for (int i = 0; i < 4; i++) {
            float alpha = 0.25f - i * 0.05f;
            shape.setColor(0.2f, 0.55f, 0.9f, alpha);
            float yBase = H * 0.18f + i * 14f;
            float amp   = 6f;
            int   steps = (int)(W / 4f);
            for (int s = 0; s < steps; s++) {
                float px  = s * 4f;
                float wave = (float) Math.sin((px + waveOffset + i * 40f) * 0.04f) * amp;
                shape.rect(px, yBase + wave, 4f, 10f);
            }
        }

        // Road stripes (decorative centre lines)
        shape.setColor(0.3f, 0.32f, 0.35f, 1f);
        shape.rect(0, H * 0.23f, W, H * 0.04f);
        shape.setColor(1f, 0.9f, 0f, 1f);
        for (int i = 0; i < 10; i++) {
            float rx = i * (W / 9f);
            shape.rect(rx, H * 0.24f, W / 22f, 4f);
        }

        shape.end();

        // Text
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        // Title
        font.getData().setScale(5f);
        font.setColor(0.05f, 0.1f, 0.5f, 1f);
        String title = "RETRO KART";
        layout.setText(font, title);
        font.draw(batch, title, (W - layout.width) * 0.5f + 3f, H * 0.78f - 3f); // shadow
        font.setColor(1f, 0.92f, 0.1f, 1f);
        font.draw(batch, title, (W - layout.width) * 0.5f, H * 0.78f);

        // Subtitle
        font.getData().setScale(1.8f);
        font.setColor(1f, 1f, 1f, 0.9f);
        String sub = "Beach Sprint  |  90 Seconds to Glory";
        layout.setText(font, sub);
        font.draw(batch, sub, (W - layout.width) * 0.5f, H * 0.62f);

        // Story teaser
        font.getData().setScale(1.2f);
        font.setColor(0.15f, 0.1f, 0.4f, 0.85f);
        String story = "Race across Kart Cove before the tide cuts the road!";
        layout.setText(font, story);
        font.draw(batch, story, (W - layout.width) * 0.5f, H * 0.52f);

        font.getData().setScale(1.3f);
        font.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        String ctrl = "ARROWS = Drive     Z = Drift     R = Restart";
        layout.setText(font, ctrl);
        font.draw(batch, ctrl, (W - layout.width) * 0.5f, H * 0.38f);

        // Pulsing "Press ENTER" prompt
        float pulse = 0.6f + 0.4f * (float) Math.sin(pulseTimer * 3.5f);
        font.getData().setScale(2.2f);
        font.setColor(1f, 1f, 1f, pulse);
        String prompt = "Press ENTER -> Enter Name -> Race!";
        layout.setText(font, prompt);
        font.draw(batch, prompt, (W - layout.width) * 0.5f, H * 0.28f);

        // Leaderboard shortcut
        font.getData().setScale(1.2f);
        font.setColor(0.8f, 0.9f, 1f, 0.75f + 0.25f * (float) Math.sin(pulseTimer * 2f));
        String lbHint = "L  =  Leaderboard";
        layout.setText(font, lbHint);
        font.draw(batch, lbHint, (W - layout.width) * 0.5f, H * 0.18f);

        font.getData().setScale(1f);
        batch.end();

        // Input
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
         || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            Gdx.app.postRunnable(() -> { game.setScreen(new NameEntryScreen(game)); dispose(); });
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            Gdx.app.postRunnable(() -> { game.setScreen(new LeaderboardScreen(game)); dispose(); });
        }
    }

    @Override public void resize(int w, int h) {
        cam.setToOrtho(false, w, h);
    }
    @Override public void show()   {}
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
    @Override public void dispose() {
        font.dispose();
        shape.dispose();
    }
}
