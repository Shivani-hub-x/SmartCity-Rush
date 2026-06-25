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
import com.retrokart.exception.RaceException;
import com.retrokart.utils.GenericLeaderboard;
import com.retrokart.utils.RaceValidator;
import com.retrokart.utils.ResultExporter;

import java.util.List;

/**
 * ResultScreen - polished post-race dashboard.
 * All layout variables are class fields (no duplicates in render).
 */
public class ResultScreen implements Screen {

    private final RetroKartGame  game;
    private final SpriteBatch    batch;
    private final BitmapFont     titleFont;
    private final BitmapFont     bodyFont;
    private final BitmapFont     smallFont;
    private final GlyphLayout    layout = new GlyphLayout();
    private final ShapeRenderer  shape;
    private final OrthographicCamera cam;
    private final Matrix4        ortho  = new Matrix4();

    private final String  playerName;
    private final float   finishTime;
    private final boolean isWin;

    // Accent colour (set once in constructor, used by both shape and batch phases)
    private final float accentR, accentG, accentB;

    private List<RaceResult>               topResults;
    private int                            playerRank = -1;
    private final GenericLeaderboard<Float> inMemoryBoard = new GenericLeaderboard<>(10, true);
    private final ResultExporter            exporter      = new ResultExporter();

    private float   pulseTimer = 0f;
    private float   starTimer  = 0f;
    private boolean exported   = false;

    // Confetti particles
    private static final int STAR_COUNT = 18;
    private final float[]   starX   = new float[STAR_COUNT];
    private final float[]   starY   = new float[STAR_COUNT];
    private final float[]   starSpd = new float[STAR_COUNT];
    private final float[]   starSz  = new float[STAR_COUNT];
    private final float[][] starCol = new float[STAR_COUNT][3];

    // Layout – computed once per resize, shared between shape and batch phases
    private float W, H;
    private float cardX, cardY, cardW, cardH, cardTop, bannerH;
    private float lbPanelX, lbPanelY, lbPanelW, lbPanelH;
    private float box1X,  box1Y, boxBW, boxBH, box2X;
    private float lcol1, lcol2, lcol3, lcol4, lrowTop, lrowH;

    public ResultScreen(RetroKartGame game, String playerName,
                        float finishTime, boolean isWin) {
        this.game       = game;
        this.batch      = game.batch;
        this.playerName = playerName;
        this.finishTime = finishTime;
        this.isWin      = isWin;

        accentR = isWin ? 0.2f : 0.9f;
        accentG = isWin ? 1.0f : 0.2f;
        accentB = isWin ? 0.3f : 0.2f;

        titleFont = new BitmapFont();
        bodyFont  = new BitmapFont();
        smallFont = new BitmapFont();
        shape     = new ShapeRenderer();
        cam       = new OrthographicCamera();
        cam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        recomputeLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        initStars();
        loadData();
    }

    private void recomputeLayout(float w, float h) {
        W = w;  H = h;
        cardX    = W * 0.06f;
        cardW    = W * 0.88f;
        cardY    = H * 0.08f;
        cardH    = H * 0.86f;
        cardTop  = cardY + cardH;
        bannerH  = H * 0.115f;

        lbPanelX = cardX + W * 0.04f;
        lbPanelW = cardW - W * 0.08f;
        lbPanelY = cardY + H * 0.17f;
        lbPanelH = H * 0.28f;

        box1X = cardX + cardW * 0.08f;
        box1Y = cardY + H * 0.48f;
        boxBW = cardW * 0.38f;
        boxBH = H * 0.12f;
        box2X = cardX + cardW * 0.54f;

        lcol1   = lbPanelX + lbPanelW * 0.04f;
        lcol2   = lbPanelX + lbPanelW * 0.14f;
        lcol3   = lbPanelX + lbPanelW * 0.65f;
        lcol4   = lbPanelX + lbPanelW * 0.80f;
        lrowTop = lbPanelY + lbPanelH - H * 0.055f;
        lrowH   = H * 0.048f;
    }

    private void initStars() {
        float[][] palette = {
            {1f,0.84f,0f},{0.3f,1f,0.5f},{0.4f,0.8f,1f},{1f,0.4f,0.7f},{1f,1f,1f}
        };
        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i]      = (float)(Math.random() * W);
            starY[i]      = (float)(Math.random() * H * 0.6f + H * 0.3f);
            starSpd[i]    = (float)(20 + Math.random() * 55);
            starSz[i]     = (float)(4  + Math.random() * 9);
            float[] c     = palette[(int)(Math.random() * palette.length)];
            starCol[i][0] = c[0]; starCol[i][1] = c[1]; starCol[i][2] = c[2];
        }
    }

    private void loadData() {
        DatabaseManager db = DatabaseManager.getInstance();
        topResults = db.getTopResults(5);

        // Populate in-memory board from DB results (deduplicated by DB query)
        for (RaceResult r : topResults) {
            inMemoryBoard.addEntry(r.finishTime, r.playerName);
        }

        // If current player isn't already in the top list, add them so they appear
        if (isWin) {
            boolean alreadyPresent = topResults.stream()
                .anyMatch(r -> r.playerName.equalsIgnoreCase(playerName)
                            && Math.abs(r.finishTime - finishTime) < 0.15f);
            if (!alreadyPresent) {
                try {
                    float validated = RaceValidator.validateTime(finishTime);
                    inMemoryBoard.addEntry(validated, playerName);
                } catch (RaceException.TimeLimitException e) {
                    System.err.println("[ResultScreen] " + e.getMessage());
                }
            }
            playerRank = db.getPlayerBestRank(playerName);
        }
        if (isWin && !topResults.isEmpty()) { exporter.exportToFile(topResults); exported = true; }
    }

    @Override
    public void render(float dt) {
        pulseTimer += dt;
        starTimer  += dt;
        ortho.setToOrtho2D(0, 0, W, H);

        Gdx.gl.glClearColor(isWin?0.04f:0.10f, isWin?0.08f:0.02f, isWin?0.04f:0.02f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // ── PHASE 1: ShapeRenderer ────────────────────────────────────
        // Enable blending so semi-transparent colours don't render as opaque
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.setProjectionMatrix(ortho);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        // Gradient background
        for (int i = 0; i < 30; i++) {
            float bg = i / 30f;
            float y0 = i * (H / 30f), y1 = (i+1) * (H / 30f);
            if (isWin) shape.setColor(0.04f, 0.08f+bg*0.06f, 0.04f, 1f);
            else        shape.setColor(0.10f+bg*0.05f, 0.02f, 0.02f, 1f);
            shape.rect(0, y0, W, y1-y0+1f);
        }

        // Confetti particles drawn early so they appear BEHIND the card/panel
        if (isWin) {
            for (int i = 0; i < STAR_COUNT; i++) {
                starY[i] += starSpd[i] * dt * 0.5f;
                if (starY[i] > H+20) starY[i] = -10;
                float sp = 0.7f + 0.3f*(float)Math.sin(starTimer*3f+i);
                shape.setColor(starCol[i][0], starCol[i][1], starCol[i][2], sp*0.85f);
                float sz = starSz[i]*(0.8f+0.2f*sp);
                shape.triangle(starX[i], starY[i]+sz, starX[i]+sz*0.55f, starY[i], starX[i]-sz*0.55f, starY[i]);
                shape.triangle(starX[i], starY[i]-sz, starX[i]+sz*0.55f, starY[i], starX[i]-sz*0.55f, starY[i]);
            }
        }

        // Card shadow + body
        shape.setColor(0f, 0f, 0f, 0.5f);  shape.rect(cardX+5, cardY-5, cardW, cardH);
        shape.setColor(0.06f, 0.06f, 0.10f, 1f); shape.rect(cardX, cardY, cardW, cardH);

        // Banner gradient
        for (int i = 0; i < (int)bannerH; i++) {
            float bt = i / bannerH;
            if (isWin) shape.setColor(0.05f+bt*0.10f, 0.35f+bt*0.20f, 0.05f, 1f);
            else        shape.setColor(0.35f+bt*0.10f, 0.04f, 0.04f, 1f);
            shape.rect(cardX, cardTop-bannerH+i, cardW, 1.5f);
        }

        // Accent lines
        shape.setColor(accentR, accentG, accentB, 0.7f);
        shape.rect(cardX, cardTop-bannerH-2, cardW, 2.5f);
        shape.setColor(accentR*0.5f, accentG*0.5f, accentB*0.5f, 0.4f);
        shape.rect(cardX, cardY+H*0.14f, cardW, 1f);

        // Leaderboard panel
        shape.setColor(0f,0f,0f,0.35f);      shape.rect(lbPanelX, lbPanelY, lbPanelW, lbPanelH);
        shape.setColor(accentR,accentG,accentB,0.7f); shape.rect(lbPanelX, lbPanelY, 3f, lbPanelH);

        // Stat boxes (win only)
        if (isWin) {
            shape.setColor(0f,0f,0f,0.4f);   shape.rect(box1X, box1Y, boxBW, boxBH);
            shape.setColor(1f,0.75f,0f,0.8f);shape.rect(box1X, box1Y, boxBW, 3f);
            shape.setColor(0f,0f,0f,0.4f);   shape.rect(box2X, box1Y, boxBW, boxBH);
            shape.setColor(accentR,accentG,accentB,0.8f); shape.rect(box2X, box1Y, boxBW, 3f);
        }

        // Leaderboard row alternating tints (now with blending enabled, alpha works correctly)
        List<GenericLeaderboard.Entry<Float>> inMemTop = inMemoryBoard.getTopEntries(5);
        for (int i = 0; i < inMemTop.size(); i++) {
            if (i % 2 == 0) {
                float ry = lrowTop - lrowH*(i+1);
                shape.setColor(1f,1f,1f,0.04f);
                shape.rect(lbPanelX+3f, ry-2f, lbPanelW-6f, lrowH-2f);
            }
        }

        shape.end(); // ← ShapeRenderer done
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── PHASE 2: SpriteBatch (text) ───────────────────────────────
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        // Title
        titleFont.getData().setScale(4.2f);
        String header = isWin ? "RACE COMPLETE!" : "TIME'S UP!";
        float headerY = cardTop - bannerH * 0.18f;
        titleFont.setColor(0f, 0f, 0f, 0.5f);
        layout.setText(titleFont, header);
        titleFont.draw(batch, header, (W-layout.width)*0.5f+2f, headerY-2f);
        if (isWin) titleFont.setColor(0.25f, 1f, 0.45f, 1f);
        else        titleFont.setColor(1f, 0.35f, 0.35f, 1f);
        titleFont.draw(batch, header, (W-layout.width)*0.5f, headerY);

        // Player name
        bodyFont.getData().setScale(1.7f);
        bodyFont.setColor(0.75f, 0.85f, 1f, 0.95f);
        String nameLabel = "Racer:  " + playerName;
        layout.setText(bodyFont, nameLabel);
        bodyFont.draw(batch, nameLabel, (W-layout.width)*0.5f, cardTop-bannerH-H*0.06f);

        if (isWin) {
            // Time box label
            smallFont.getData().setScale(0.9f);
            smallFont.setColor(0.7f, 0.7f, 0.7f, 0.8f);
            layout.setText(smallFont, "FINISH TIME");
            smallFont.draw(batch, "FINISH TIME", box1X+(boxBW-layout.width)*0.5f, box1Y+boxBH-6f);
            // Time value
            bodyFont.getData().setScale(2.3f);
            bodyFont.setColor(1f, 0.85f, 0.1f, 1f);
            String timeStr = RaceValidator.formatTime(finishTime);
            layout.setText(bodyFont, timeStr);
            bodyFont.draw(batch, timeStr, box1X+(boxBW-layout.width)*0.5f, box1Y+boxBH*0.62f);

            // Rank box label
            smallFont.getData().setScale(0.9f);
            smallFont.setColor(0.7f, 0.7f, 0.7f, 0.8f);
            layout.setText(smallFont, "YOUR RANK");
            smallFont.draw(batch, "YOUR RANK", box2X+(boxBW-layout.width)*0.5f, box1Y+boxBH-6f);
            // Rank value
            bodyFont.getData().setScale(2.3f);
            String rankVal = (playerRank > 0) ? "#"+playerRank+RaceValidator.rankSuffix(playerRank) : "---";
            if (playerRank > 0) bodyFont.setColor(accentR, accentG, accentB, 1f);
            else                 bodyFont.setColor(0.6f, 0.6f, 0.6f, 1f);
            layout.setText(bodyFont, rankVal);
            bodyFont.draw(batch, rankVal, box2X+(boxBW-layout.width)*0.5f, box1Y+boxBH*0.62f);

            if (exported) {
                // silently saved — no annotation label shown
            }
        } else {
            bodyFont.getData().setScale(1.5f);
            bodyFont.setColor(0.9f, 0.55f, 0.55f, 1f);
            String msg = "You didn't reach the finish in time.";
            layout.setText(bodyFont, msg);
            bodyFont.draw(batch, msg, (W-layout.width)*0.5f, cardTop-bannerH-H*0.18f);

            bodyFont.getData().setScale(1.2f);
            bodyFont.setColor(0.7f, 0.7f, 0.7f, 0.75f);
            String tip = "Tip: follow the yellow road and drift on corners!";
            layout.setText(bodyFont, tip);
            bodyFont.draw(batch, tip, (W-layout.width)*0.5f, cardTop-bannerH-H*0.28f);
        }

        // Leaderboard title
        smallFont.getData().setScale(1.0f);
        smallFont.setColor(accentR*0.8f+0.2f, accentG*0.8f+0.2f, accentB*0.8f+0.2f, 1f);
        String lbTitle = "  Top 5  ";
        layout.setText(smallFont, lbTitle);
        smallFont.draw(batch, lbTitle, lbPanelX+(lbPanelW-layout.width)*0.5f, lbPanelY+lbPanelH-6f);

        // Column headers
        smallFont.getData().setScale(0.85f);
        smallFont.setColor(0.55f, 0.65f, 0.75f, 0.9f);
        smallFont.draw(batch, "#",      lcol1, lrowTop);
        smallFont.draw(batch, "Player", lcol2, lrowTop);
        smallFont.draw(batch, "Time",   lcol3, lrowTop);
        smallFont.draw(batch, "Date",   lcol4, lrowTop);

        // Leaderboard rows
        for (int i = 0; i < inMemTop.size(); i++) {
            GenericLeaderboard.Entry<Float> e = inMemTop.get(i);
            float ry   = lrowTop - lrowH*(i+1);
            boolean isMe = isWin && i==0
                        && e.playerName.equalsIgnoreCase(playerName)
                        && Math.abs(e.score-finishTime) < 0.15f;

            smallFont.getData().setScale(isMe ? 1.0f : 0.92f);
            if (i==0)      smallFont.setColor(1f, 0.84f, 0f, 1f);
            else if (i==1) smallFont.setColor(0.78f, 0.78f, 0.78f, 1f);
            else if (i==2) smallFont.setColor(0.80f, 0.50f, 0.20f, 1f);
            else           smallFont.setColor(0.85f, 0.85f, 0.85f, isMe?1f:0.75f);

            smallFont.draw(batch, "#"+(i+1),                         lcol1, ry);
            String nameText = e.playerName + (isMe ? "  < you" : "");
            smallFont.draw(batch, nameText,                           lcol2, ry);
            smallFont.draw(batch, RaceValidator.formatTime(e.score),  lcol3, ry);
            String dateStr = "";
            if (i < topResults.size()) {
                String rd = topResults.get(i).raceDate;
                dateStr = (rd!=null && rd.length()>=10) ? rd.substring(0,10) : "";
            }
            smallFont.draw(batch, dateStr, lcol4, ry);

        }

        // Nav
        float navPulse = 0.6f + 0.4f*(float)Math.sin(pulseTimer*3.2f);
        bodyFont.getData().setScale(1.3f);
        bodyFont.setColor(accentR, accentG, accentB, navPulse);
        String nav = "  [R] Race Again     [L] Leaderboard     [ESC] Menu  ";
        layout.setText(bodyFont, nav);
        bodyFont.draw(batch, nav, (W-layout.width)*0.5f, cardY+H*0.09f);

        titleFont.getData().setScale(1f);
        bodyFont.getData().setScale(1f);
        smallFont.getData().setScale(1f);
        batch.end();

        // Input
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.postRunnable(() -> { game.setScreen(new MenuScreen(game)); dispose(); });
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            Gdx.app.postRunnable(() -> { game.setScreen(new NameEntryScreen(game)); dispose(); });
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            Gdx.app.postRunnable(() -> { game.setScreen(new LeaderboardScreen(game)); dispose(); });
        }
    }

    @Override public void resize(int w, int h) {
        cam.setToOrtho(false, w, h);
        ortho.setToOrtho2D(0, 0, w, h);
        recomputeLayout(w, h);
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
