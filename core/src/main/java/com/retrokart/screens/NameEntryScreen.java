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
import com.retrokart.RetroKartGame;
import com.retrokart.exception.RaceException;
import com.retrokart.utils.RaceValidator;

/**
 * NameEntryScreen – player types their name before racing.
 *
 * Syllabus coverage (TCS-408):
 *  Unit 1 – StringBuffer: accumulates typed characters, supports backspace delete.
 *           Input methods  : keyboard character-by-character input.
 *           String handling: String vs StringBuffer comparison.
 *  Unit 3 – Custom exception: RaceValidator.validateName() throws
 *            InvalidNameException; caught here with try-catch.
 *  Unit 2 – Encapsulation: playerName stored and passed to next screen.
 */
public class NameEntryScreen implements Screen {

    private final RetroKartGame   game;
    private final SpriteBatch     batch;
    private final BitmapFont      font;
    private final GlyphLayout     layout = new GlyphLayout();
    private final ShapeRenderer   shape;
    private final OrthographicCamera cam;

    // Unit 1: StringBuffer for mutable live-typed input
    private final StringBuffer nameBuffer = new StringBuffer();

    private String  errorMessage = "";
    private float   errorTimer   = 0f;
    private float   pulseTimer   = 0f;
    private float   cursorBlink  = 0f;
    private float   waveOffset   = 0f;

    private static final int MAX_CHARS = 12;

    public NameEntryScreen(RetroKartGame game) {
        this.game  = game;
        this.batch = game.batch;
        this.font  = new BitmapFont();
        this.shape = new ShapeRenderer();
        this.cam   = new OrthographicCamera();
        cam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Default name pre-filled
        nameBuffer.append("Player");
    }

    @Override
    public void render(float dt) {
        pulseTimer  += dt;
        cursorBlink += dt;
        waveOffset  += dt * 55f;
        if (errorTimer > 0f) errorTimer -= dt;

        handleInput();

        float W = Gdx.graphics.getWidth();
        float H = Gdx.graphics.getHeight();

        // Background
        Gdx.gl.glClearColor(0.53f, 0.81f, 0.98f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Waves
        shape.setProjectionMatrix(cam.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.96f, 0.87f, 0.60f, 1f);
        shape.rect(0, 0, W, H * 0.15f);
        for (int i = 0; i < 3; i++) {
            shape.setColor(0.2f, 0.55f, 0.9f, 0.2f - i * 0.04f);
            float yBase = H * 0.12f + i * 12f;
            int steps = (int)(W / 4f);
            for (int s = 0; s < steps; s++) {
                float px   = s * 4f;
                float wave = (float) Math.sin((px + waveOffset + i * 30f) * 0.04f) * 6f;
                shape.rect(px, yBase + wave, 4f, 9f);
            }
        }
        // Input box background
        float boxW = W * 0.55f, boxH = 55f;
        float boxX = (W - boxW) * 0.5f, boxY = H * 0.42f;
        shape.setColor(0.08f, 0.08f, 0.25f, 0.75f);
        shape.rect(boxX, boxY, boxW, boxH);
        shape.setColor(0.3f, 0.7f, 1.0f, 1f);
        // Border lines
        shape.rectLine(boxX, boxY, boxX + boxW, boxY, 2f);
        shape.rectLine(boxX, boxY + boxH, boxX + boxW, boxY + boxH, 2f);
        shape.rectLine(boxX, boxY, boxX, boxY + boxH, 2f);
        shape.rectLine(boxX + boxW, boxY, boxX + boxW, boxY + boxH, 2f);
        shape.end();

        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        // Title
        font.getData().setScale(3.5f);
        font.setColor(0.1f, 0.1f, 0.5f, 1f);
        String title = "ENTER YOUR NAME";
        layout.setText(font, title);
        float tx = (W - layout.width) * 0.5f;
        font.draw(batch, title, tx + 2f, H * 0.85f - 2f);
        font.setColor(1f, 0.92f, 0.1f, 1f);
        font.draw(batch, title, tx, H * 0.85f);

        // Subtitle
        font.getData().setScale(1.3f);
        font.setColor(1f, 1f, 1f, 0.85f);
        String sub = "Your name appears on the leaderboard!";
        layout.setText(font, sub);
        font.draw(batch, sub, (W - layout.width) * 0.5f, H * 0.72f);

        // Prompt label
        font.getData().setScale(1.4f);
        font.setColor(0.6f, 0.9f, 1f, 1f);
        String label = "Racer Name:";
        layout.setText(font, label);
        font.draw(batch, label, (W - layout.width) * 0.5f, H * 0.57f);

        // Typed name (StringBuffer content)
        font.getData().setScale(2.0f);
        font.setColor(1f, 1f, 1f, 1f);
        // StringBuffer.toString() to get current string (Unit 1)
        String current = nameBuffer.toString();
        // Show cursor blink
        boolean showCursor = (cursorBlink % 1.0f) < 0.55f;
        String display = current + (showCursor ? "|" : " ");
        layout.setText(font, display);
        font.draw(batch, display, (W - layout.width) * 0.5f, boxY + boxH * 0.72f);

        // Char counter (Wrapper class: Integer.toString, Unit 1)
        font.getData().setScale(0.9f);
        font.setColor(0.6f, 0.8f, 0.6f, 0.7f);
        String counter = Integer.toString(nameBuffer.length()) + "/" + MAX_CHARS;
        layout.setText(font, counter);
        font.draw(batch, counter, boxX + boxW - layout.width - 6f, boxY - 4f);

        // Error message
        if (errorTimer > 0f) {
            font.getData().setScale(1.3f);
            font.setColor(1f, 0.3f, 0.3f, Math.min(errorTimer, 1f));
            layout.setText(font, errorMessage);
            font.draw(batch, errorMessage, (W - layout.width) * 0.5f, H * 0.36f);
        }

        // Instructions
        font.getData().setScale(1.1f);
        font.setColor(0.85f, 0.95f, 0.85f, 0.85f);
        String hint = "Type name, then ENTER to race  |  ESC = back";
        layout.setText(font, hint);
        font.draw(batch, hint, (W - layout.width) * 0.5f, H * 0.26f);

        // Pulsing ENTER prompt
        float pulse = 0.55f + 0.45f * (float) Math.sin(pulseTimer * 3.5f);
        font.getData().setScale(1.7f);
        font.setColor(0.2f, 0.9f, 0.3f, pulse);
        String go = "ENTER  ->  Race!";
        layout.setText(font, go);
        font.draw(batch, go, (W - layout.width) * 0.5f, H * 0.16f);

        font.getData().setScale(1f);
        batch.end();
    }

    // ── Input handling ────────────────────────────────────────────────

    private void handleInput() {
        // ESC: back to menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
            dispose();
            return;
        }

        // ENTER: validate and proceed
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            attemptConfirm();
            return;
        }

        // BACKSPACE: StringBuffer.deleteCharAt() to remove last char (Unit 1)
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            if (nameBuffer.length() > 0) {
                nameBuffer.deleteCharAt(nameBuffer.length() - 1);
            }
            return;
        }

        // Character keys: append to StringBuffer (Unit 1)
        if (nameBuffer.length() < MAX_CHARS) {
            // Letters A-Z
            for (int k = Input.Keys.A; k <= Input.Keys.Z; k++) {
                if (Gdx.input.isKeyJustPressed(k)) {
                    char c = (char)('A' + (k - Input.Keys.A));
                    if (!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                     && !Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) {
                        c = Character.toLowerCase(c);
                    }
                    // StringBuffer.append() — mutable string operation (Unit 1)
                    nameBuffer.append(c);
                }
            }
            // Digits 0-9
            for (int k = Input.Keys.NUM_0; k <= Input.Keys.NUM_9; k++) {
                if (Gdx.input.isKeyJustPressed(k)) {
                    nameBuffer.append((char)('0' + (k - Input.Keys.NUM_0)));
                }
            }
            // Underscore and dash
            if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
                nameBuffer.append(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ? '_' : '-');
            }
            // Space
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                nameBuffer.append(' ');
            }
        }
    }

    /** Validates name using RaceValidator and navigates to RaceScreen. */
    private void attemptConfirm() {
        // Unit 3: try-catch with custom checked exception
        try {
            // throws InvalidNameException if name is invalid
            final String validated = RaceValidator.validateName(nameBuffer.toString());
            // Defer screen switch to next frame to avoid mid-render disposal crash
            Gdx.app.postRunnable(() -> {
                game.setScreen(new RaceScreen(game, validated));
                dispose();
            });
        } catch (RaceException.InvalidNameException e) {
            // Display the error message from the custom exception
            errorMessage = e.getMessage();
            errorTimer   = 3.0f;
            System.err.println("[NameEntry] " + e.getMessage());
        }
    }

    @Override public void resize(int w, int h) { cam.setToOrtho(false, w, h); }
    @Override public void show()   {}
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
    @Override public void dispose() {
        // Do NOT dispose game.batch - it is shared and managed by RetroKartGame
        font.dispose();
        shape.dispose();
    }
}
