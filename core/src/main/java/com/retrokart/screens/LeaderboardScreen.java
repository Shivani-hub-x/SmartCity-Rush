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
import com.retrokart.db.DatabaseManager;
import com.retrokart.db.DatabaseManager.RaceResult;
import com.retrokart.utils.RaceValidator;

import java.util.List;

public class LeaderboardScreen implements Screen {

    private final RetroKartGame   game;
    private final SpriteBatch     batch;
    private final BitmapFont      titleFont;
    private final BitmapFont      bodyFont;
    private final BitmapFont      smallFont;
    private final GlyphLayout     layout  = new GlyphLayout();
    private final ShapeRenderer   shape;
    private final OrthographicCamera cam;
    private final Matrix4         ortho   = new Matrix4();

    private List<RaceResult> results;
    private int              totalRaces;
    private float pulseTimer = 0f;
    private float waveTimer  = 0f;

    public LeaderboardScreen(RetroKartGame game) {
        this.game  = game;
        this.batch = game.batch;
        titleFont  = new BitmapFont();
        bodyFont   = new BitmapFont();
        smallFont  = new BitmapFont();
        shape      = new ShapeRenderer();
        cam        = new OrthographicCamera();
        cam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        DatabaseManager db = DatabaseManager.getInstance();
        results    = db.getTopResults(10);
        totalRaces = db.getTotalRaces();
    }

    @Override
    public void render(float dt) {
        pulseTimer += dt;
        waveTimer  += dt;
        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();
        ortho.setToOrtho2D(0, 0, W, H);

        Gdx.gl.glClearColor(0.04f, 0.06f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shape.setProjectionMatrix(ortho);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        // Gradient background
        int strips = 20;
        for (int i = 0; i < strips; i++) {
            float t = i / (float)strips;
            shape.setColor(0.04f, 0.06f + t * 0.04f, 0.12f + t * 0.06f, 1f);
            shape.rect(0, i * (H / strips), W, H / strips + 1f);
        }

        // Wave footer
        shape.setColor(0.08f, 0.15f, 0.30f, 0.6f);
        shape.rect(0, 0, W, H * 0.07f);
        for (int i = 0; i < (int)(W / 3f); i++) {
            float px   = i * 3f;
            float wave = (float)Math.sin((px + waveTimer * 60f) * 0.035f) * 5f;
            shape.setColor(0.15f, 0.40f, 0.80f, 0.25f);
            shape.rect(px, H * 0.055f + wave - 4f, 3f, 8f);
        }

        // Card
        float cardX = W * 0.05f, cardW = W * 0.90f;
        float cardY = H * 0.10f, cardH = H * 0.84f;
        float cardTop = cardY + cardH;

        shape.setColor(0f, 0f, 0f, 0.45f);
        shape.rect(cardX + 4f, cardY - 4f, cardW, cardH);
        shape.setColor(0.06f, 0.07f, 0.13f, 1f);
        shape.rect(cardX, cardY, cardW, cardH);

        // Banner
        float bannerH = H * 0.11f;
        for (int i = 0; i < (int)bannerH; i++) {
            float t = i / bannerH;
            shape.setColor(0.05f + t * 0.05f, 0.12f + t * 0.18f, 0.30f + t * 0.20f, 1f);
            shape.rect(cardX, cardTop - bannerH + i, cardW, 1.5f);
        }
        // Gold accent line at BOTTOM of banner (not crossing the title text)
        shape.setColor(1f, 0.84f, 0f, 0.9f);
        shape.rect(cardX, cardTop - bannerH - 2f, cardW, 3f);

        // Pulsing corner circles
        float pulse = 0.7f + 0.3f * (float)Math.sin(pulseTimer * 2.5f);
        for (float tx2 : new float[]{cardX + 28f, cardX + cardW - 28f}) {
            shape.setColor(1f, 0.84f, 0f, pulse);
            shape.circle(tx2, cardTop - bannerH * 0.5f, 7f);
            shape.setColor(0.9f, 0.6f, 0f, pulse * 0.5f);
            shape.circle(tx2, cardTop - bannerH * 0.5f, 13f);
        }

        // Table rows
        float tblX   = cardX + W * 0.03f;
        float tblW   = cardW - W * 0.06f;
        float tblTop = cardTop - bannerH - H * 0.055f;
        float rowH   = H * 0.062f;
        int   maxRows= Math.min(results.size(), 10);

        for (int i = 0; i < maxRows; i++) {
            float rowY = tblTop - rowH * (i + 1);
            if (i % 2 == 0) {
                shape.setColor(1f, 1f, 1f, 0.03f);
                shape.rect(tblX, rowY - 2f, tblW, rowH - 2f);
            }
        }
        // Top-3 left accent bars
        float[] medalRGB = {1f,0.84f,0f,  0.78f,0.78f,0.78f,  0.80f,0.50f,0.20f};
        for (int i = 0; i < Math.min(3, maxRows); i++) {
            float rowY = tblTop - rowH * (i + 1);
            shape.setColor(medalRGB[i*3], medalRGB[i*3+1], medalRGB[i*3+2], 0.9f);
            shape.rect(tblX, rowY - 2f, 4f, rowH - 2f);
        }
        // Header underline
        shape.setColor(0.3f, 0.5f, 0.8f, 0.5f);
        shape.rect(tblX, tblTop - rowH - 2f, tblW, 1.5f);

        shape.end();

        // Text
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        // Title inside banner
        titleFont.getData().setScale(3.6f);
        String title = "LEADERBOARD";
        titleFont.setColor(0f, 0f, 0f, 0.5f);
        layout.setText(titleFont, title);
        float tx = (W - layout.width) * 0.5f;
        float titleY = cardTop - bannerH * 0.12f;
        titleFont.draw(batch, title, tx + 2f, titleY - 2f);
        titleFont.setColor(1f, 0.92f, 0.15f, 1f);
        titleFont.draw(batch, title, tx, titleY);

        // DB status
        smallFont.getData().setScale(0.9f);
        smallFont.setColor(0.6f, 0.85f, 0.6f, 0.85f);
        String dbLine = "Total races: " + totalRaces + "  |  SQLite via JDBC (CO6)";
        layout.setText(smallFont, dbLine);
        smallFont.draw(batch, dbLine, (W - layout.width) * 0.5f, cardTop - bannerH - H * 0.022f);

        // Column headers
        float col1 = tblX + tblW * 0.01f;
        float col2 = tblX + tblW * 0.10f;
        float col3 = tblX + tblW * 0.55f;
        float col4 = tblX + tblW * 0.76f;
        smallFont.getData().setScale(0.92f);
        smallFont.setColor(0.45f, 0.65f, 0.90f, 1f);
        smallFont.draw(batch, "Rank",   col1, tblTop - rowH * 0.22f);
        smallFont.draw(batch, "Player", col2, tblTop - rowH * 0.22f);
        smallFont.draw(batch, "Time",   col3, tblTop - rowH * 0.22f);
        smallFont.draw(batch, "Date",   col4, tblTop - rowH * 0.22f);

        if (results.isEmpty()) {
            bodyFont.getData().setScale(1.6f);
            bodyFont.setColor(0.7f, 0.7f, 0.7f, 0.9f);
            String none = "No races recorded yet!";
            layout.setText(bodyFont, none);
            bodyFont.draw(batch, none, (W - layout.width) * 0.5f, cardY + cardH * 0.5f);
        } else {
            for (int i = 0; i < maxRows; i++) {
                RaceResult r = results.get(i);
                float rowY   = tblTop - rowH * (i + 1.1f);

                smallFont.getData().setScale(i < 3 ? 1.0f : 0.94f);
                if (i == 0)      smallFont.setColor(1f, 0.84f, 0f, 1f);
                else if (i == 1) smallFont.setColor(0.80f, 0.80f, 0.80f, 1f);
                else if (i == 2) smallFont.setColor(0.80f, 0.50f, 0.20f, 1f);
                else             smallFont.setColor(0.80f, 0.85f, 0.90f, 0.85f);

                smallFont.draw(batch, "#" + r.rank, col1, rowY);
                smallFont.draw(batch, r.playerName,  col2, rowY);
                smallFont.draw(batch, RaceValidator.formatTime(r.finishTime), col3, rowY);
                String dateStr = (r.raceDate != null && r.raceDate.length() >= 10)
                        ? r.raceDate.substring(0, 10) : "--";
                smallFont.draw(batch, dateStr, col4, rowY);
            }
        }

        // Back prompt
        float p2 = 0.55f + 0.45f * (float)Math.sin(pulseTimer * 3f);
        bodyFont.getData().setScale(1.25f);
        bodyFont.setColor(0.3f, 0.8f, 1f, p2);
        String back = "[ESC] / [BACKSPACE]  -  Back to Menu";
        layout.setText(bodyFont, back);
        bodyFont.draw(batch, back, (W - layout.width) * 0.5f, cardY + H * 0.052f);

        titleFont.getData().setScale(1f);
        bodyFont.getData().setScale(1f);
        smallFont.getData().setScale(1f);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
         || Gdx.input.isKeyJustPressed(Input.Keys.BACK)
         || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            Gdx.app.postRunnable(() -> { game.setScreen(new MenuScreen(game)); dispose(); });
        }
    }

    @Override public void resize(int w, int h) {
        cam.setToOrtho(false, w, h);
        ortho.setToOrtho2D(0, 0, w, h);
    }
    @Override public void show()   {}
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
    @Override public void dispose() {
        titleFont.dispose();
        bodyFont.dispose();
        smallFont.dispose();
        shape.dispose();
    }
}
