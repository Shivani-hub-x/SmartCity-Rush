package com.retrokart.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.retrokart.RetroKartGame;
import com.retrokart.audio.SoundManager;
import com.retrokart.db.DatabaseManager;
import com.retrokart.entities.Entity;
import com.retrokart.entities.ItemBox;
import com.retrokart.entities.Kart;
import com.retrokart.entities.BotKart;
import com.retrokart.physics.TerrainSampler3D;
import com.retrokart.rendering.Camera3D;
import com.retrokart.rendering.EntityRenderer;
import com.retrokart.rendering.HUD;
import com.retrokart.rendering.ParticleSystem;
import com.retrokart.rendering.Track3DRenderer;
import com.retrokart.physics.TrackSurface;
import com.retrokart.utils.AssetFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * RaceScreen - single-player beach race.
 *
 * Features 1-5 (from previous version) plus:
 *  6. Skid mark trails drawn while drifting
 *  7. Speed boost item boxes on the track
 *  8. Main menu (MenuScreen launched from RetroKartGame)
 *  9. Procedural sound effects via SoundManager
 */
public class RaceScreen implements Screen {

    private final RetroKartGame game;
    private final String        playerName;   // Unit 2: encapsulated, passed from NameEntryScreen

    // Rendering
    private Track3DRenderer    trackRenderer;
    private EntityRenderer     entityRenderer;
    private HUD                hud;
    private SpriteBatch        batch;
    private OrthographicCamera orthoCamera;

    // 3D world
    private Camera3D           camera;
    private ModelInstance      trackInstance;
    private TerrainSampler3D   terrainSampler;
    private ParticleSystem     particleSystem;

    // Entities
    private Kart               playerKart;
    private BotKart            bowserBot;
    private List<Entity>       allEntities  = new ArrayList<>();
    private List<ItemBox>      itemBoxes    = new ArrayList<>();  // Feature 7

    // Race state
    private static final float RACE_DURATION = 90f;   // 90s gives comfortable time to finish
    private float   raceTimer    = RACE_DURATION;
    private boolean raceFinished = false;
    private boolean playerWon    = false;

    // Feature 1: Countdown
    private float   countdownTimer   = 3.0f;
    private boolean countdownDone    = false;
    private int     lastCountBeep    = 4;   // triggers beeps at 3,2,1

    // Feature 5: Best time
    private static final String PREF_FILE = "retrokart_prefs";
    private static final String PREF_BEST = "best_time";
    private float   bestTime    = 0f;
    private boolean newBestTime = false;

    // Sprint target
    private float startX, startZ;
    private boolean hasSaved = false;  // guard: save DB result exactly once
    static final float END_X = -1945.4f;
    static final float END_Z = -949.7f;

    // Feature 4: Finish billboard
    private Entity  finishFlag;
    private Texture finishFlagTex;

    // Feature 6: Skid marks
    private static final int   MAX_SKID_MARKS = 300;
    private final float[]      skidX  = new float[MAX_SKID_MARKS];
    private final float[]      skidZ  = new float[MAX_SKID_MARKS];
    private final float[]      skidA  = new float[MAX_SKID_MARKS];  // alpha
    private int                skidCount   = 0;
    private float              skidStampTimer = 0f;
    private static final float SKID_STAMP_INTERVAL = 0.06f;

    // Feature 9: Sound
    private SoundManager soundManager;
    
    // Collision logic
    private float collisionCooldown = 0f;

    /** Default constructor – uses "Player" as name (Unit 2: constructor overloading). */
    public RaceScreen(RetroKartGame game) {
        this(game, "Player");
    }

    /** Primary constructor with player name from NameEntryScreen. */
    public RaceScreen(RetroKartGame game, String playerName) {
        this.game       = game;
        this.playerName = (playerName != null && !playerName.isBlank()) ? playerName : "Player";
        loadBestTime();
        soundManager = new SoundManager();
        init();
    }

    // ── Preferences ──────────────────────────────────────────────────

    private void loadBestTime() {
        Preferences prefs = Gdx.app.getPreferences(PREF_FILE);
        bestTime = prefs.getFloat(PREF_BEST, 0f);
    }

    private void saveBestTime(float elapsed) {
        Preferences prefs = Gdx.app.getPreferences(PREF_FILE);
        prefs.putFloat(PREF_BEST, elapsed);
        prefs.flush();
        bestTime = elapsed;
    }

    // ── Init ─────────────────────────────────────────────────────────

    private void init() {
        raceTimer      = RACE_DURATION;
        raceFinished   = false;
        playerWon      = false;
        countdownTimer = 3.0f;
        countdownDone  = false;
        newBestTime    = false;
        lastCountBeep  = 4;
        hasSaved       = false;
        skidCount      = 0;
        skidStampTimer = 0f;
        allEntities.clear();
        itemBoxes.clear();

        batch          = game.batch;
        trackRenderer  = new Track3DRenderer();
        entityRenderer = new EntityRenderer();
        hud            = new HUD();
        particleSystem = new ParticleSystem();

        orthoCamera = new OrthographicCamera();
        orthoCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        trackInstance = new ModelInstance(AssetFactory.getTrackModel());
        float s = AssetFactory.MODEL_SCALE;
        trackInstance.transform.setToScaling(s, s, s);

        terrainSampler = new TerrainSampler3D(
                AssetFactory.getTrackModel(), trackInstance.transform);

        camera = new Camera3D(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        startX = -880.1f;
        startZ = -1639.6f;
        float initY = terrainSampler.getHeight(startX, startZ);

        playerKart = new Kart(
                AssetFactory.getPlayerKartSheet(),
                AssetFactory.PLAYER_FRAME_W, AssetFactory.PLAYER_FRAME_H,
                AssetFactory.PLAYER_ROWS,
                startX, startZ,
                new Color(0.2f, 0.4f, 1.0f, 1f),
                terrainSampler, true, true);
        playerKart.angle  = -MathUtils.PI / 2f;
        playerKart.worldY = initY;
        allEntities.add(playerKart);

        // Bowser bot – BotKart: slow idle spin + angle-based 22-frame selection
        float botX = -882.3f;
        float botZ = -1628.4f;
        bowserBot = new BotKart(
                AssetFactory.getBowserSheet(),
                botX, botZ,
                terrainSampler);
        bowserBot.worldY = terrainSampler.getHeight(botX, botZ);
        allEntities.add(bowserBot);

        buildFinishFlag();
        spawnItemBoxes();   // Feature 7

        camera.snapTo(playerKart.x, playerKart.worldY, playerKart.y, playerKart.angle);
    }

    // ── Feature 4: Finish flag ────────────────────────────────────────

    private void buildFinishFlag() {
        int W = 64, H = 64;
        Pixmap pm = new Pixmap(W, H, Pixmap.Format.RGBA8888);
        pm.setColor(0, 0, 0, 0);
        pm.fill();

        pm.setColor(0.2f, 0.2f, 0.2f, 1f);
        for (int py = 0; py < H; py++) {
            pm.drawPixel(W / 2, py);
            pm.drawPixel(W / 2 + 1, py);
        }

        int sq      = 8;
        int flagTop = H / 2, flagLeft = W / 2 + 2;
        for (int py = flagTop; py < H; py++) {
            for (int px = flagLeft; px < W; px++) {
                boolean white = ((px - flagLeft) / sq + (py - flagTop) / sq) % 2 == 0;
                pm.setColor(white ? 1f : 0f, white ? 1f : 0f, white ? 1f : 0f, 1f);
                pm.drawPixel(px, py);
            }
        }
        pm.setColor(0.2f, 1f, 0.3f, 1f);
        for (int px = flagLeft; px < W; px++) { pm.drawPixel(px, flagTop); pm.drawPixel(px, H-1); }
        for (int py = flagTop;  py < H; py++) { pm.drawPixel(flagLeft, py); pm.drawPixel(W-1, py); }

        finishFlagTex = new Texture(pm);
        pm.dispose();

        float flagY = terrainSampler.getHeight(END_X, END_Z);
        finishFlag = new Entity(finishFlagTex, END_X, END_Z);
        finishFlag.worldY        = flagY;
        finishFlag.spriteWorldSize = 120f;
        allEntities.add(finishFlag);
    }

    // ── Feature 7: Item boxes ─────────────────────────────────────────

    /**
     * Place 5 boost item boxes spread along the route between start and finish.
     * Positions chosen at rough intermediate points on the beach track.
     */
    private void spawnItemBoxes() {
        float[][] positions = {
            { -1100f, -1580f },
            { -1300f, -1450f },
            { -1520f, -1300f },
            { -1700f, -1150f },
            { -1850f, -1020f }
        };
        for (float[] pos : positions) {
            float bx = pos[0], bz = pos[1];
            float by = terrainSampler.getHeight(bx, bz);
            ItemBox box = new ItemBox(AssetFactory.getItemBoxTexture(), bx, bz);
            box.worldY = by;
            itemBoxes.add(box);
            allEntities.add(box);
        }
    }

    // ── Render ───────────────────────────────────────────────────────

    @Override
    public void render(float dt) {
        dt = Math.min(dt, 0.05f);
        update(dt);

        Gdx.gl.glClearColor(0.53f, 0.81f, 0.98f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        trackRenderer.render(camera, trackInstance);

        batch.setProjectionMatrix(orthoCamera.combined);

        batch.begin();
        entityRenderer.render(batch, camera, allEntities);
        batch.end();

        batch.begin();
        hud.draw(batch, playerKart,
                 Math.max(0, raceTimer), raceFinished, playerWon,
                 countdownDone, countdownTimer,
                 bestTime, newBestTime);
        batch.end();

        hud.drawDriftBar(playerKart);
        hud.drawMinimap(playerKart, startX, startZ, END_X, END_Z);

        // Feature 6: skid marks (drawn after HUD, uses ShapeRenderer)
        hud.drawSkidMarks(skidX, skidZ, skidA, skidCount, camera, playerKart.worldY);
        
        // Particles
        particleSystem.render(camera);
    }

    // ── Update ───────────────────────────────────────────────────────

    private void update(float dt) {
        // Countdown phase
        if (!countdownDone) {
            countdownTimer -= dt;

            // Feature 9: countdown beeps
            int countNow = (int) Math.ceil(countdownTimer);
            if (countNow < lastCountBeep && countNow >= 1) {
                soundManager.playCountBeep();
                lastCountBeep = countNow;
            }
            if (countdownTimer <= 0f) {
                countdownDone  = true;
                countdownTimer = 0f;
                soundManager.playGoBeep();
            }

            camera.follow(playerKart.x, playerKart.worldY, playerKart.y,
                          playerKart.angle, dt);
            return;
        }

        // Screen transitions (win/timeout) are handled by postRunnable.
        // Once raceFinished=true we stop updating but keep rendering one frame.
        if (raceFinished) {
            soundManager.update(0f, false, false, false);
            camera.follow(playerKart.x, playerKart.worldY, playerKart.y,
                          playerKart.angle, dt);
            return;
        }

        // Race timer
        raceTimer -= dt;
        if (raceTimer <= 0f && !raceFinished) {
            raceTimer    = 0f;
            raceFinished = true;
            playerWon    = false;
            Gdx.app.postRunnable(() -> {
                game.setScreen(new ResultScreen(game, playerName, 0f, false));
                dispose();
            });
        } else if (!raceFinished) {
            float dx = playerKart.x - END_X;
            float dz = playerKart.y - END_Z;
            if (dx * dx + dz * dz < 400f) {
                raceFinished = true;
                playerWon    = true;
                float elapsed = RACE_DURATION - raceTimer;
                if (bestTime == 0f || elapsed < bestTime) {
                    saveBestTime(elapsed);
                    newBestTime = true;
                }
                // JDBC: persist result exactly once (Unit 6 CO6)
                if (!hasSaved) {
                    hasSaved = true;
                    DatabaseManager.getInstance().saveResult(playerName, elapsed);
                }
                soundManager.playFinish();

                // Navigate to ResultScreen after short delay (next frame)
                final float finalElapsed = elapsed;
                Gdx.app.postRunnable(() -> {
                    game.setScreen(new ResultScreen(game, playerName, finalElapsed, true));
                    dispose();
                });
            }
        }

        if (raceFinished) return;   // don't update entities after finish triggered

        playerKart.update(dt);
        bowserBot.update(dt);   // spins + picks frame by camera angle

        // Feature 7: check item box collisions
        for (ItemBox box : itemBoxes) {
            box.update(dt);
            if (!box.isCollected() && playerKart.overlaps(box)) {
                box.collect();
                playerKart.applyPickupBoost();   // new method on Kart
                soundManager.playBoost();         // Feature 9
            }
        }

        // Feature 6: stamp skid marks + fade existing ones
        for (int i = 0; i < skidCount; i++) {
            if (skidA[i] > 0f) skidA[i] -= 0.0015f;
        }
        if (playerKart.isDrifting()) {
            skidStampTimer -= dt;
            if (skidStampTimer <= 0f) {
                skidStampTimer = SKID_STAMP_INTERVAL;
                stampSkid(playerKart.x, playerKart.y);
            }
            
            // Emit drift sparks
            float backX = playerKart.x - MathUtils.sin(playerKart.angle) * 8f;
            float backZ = playerKart.y - MathUtils.cos(playerKart.angle) * 8f;
            float vx = MathUtils.random(-5f, 5f);
            float vy = MathUtils.random(10f, 25f);
            float vz = MathUtils.random(-5f, 5f);
            // transition from yellow to orange based on skidAlpha
            float r = 1f;
            float g = MathUtils.lerp(1f, 0.4f, playerKart.skidAlpha);
            float b = 0.1f;
            particleSystem.spawnParticle(backX, playerKart.worldY + 1f, backZ, vx, vy, vz, 1.5f, r, g, b, 1f, 0.4f);
        }

        // Camera Shake & Collisions
        if (collisionCooldown > 0f) collisionCooldown -= dt;
        
        // Collision removed as per user request
        /*
        if (playerKart.resolveCollision(bowserBot)) {
            if (collisionCooldown <= 0f) {
                camera.addShake(0.08f, 0.15f); // single light bump
                soundManager.playCountBeep(); // short sound instead of long finish arpeggio
                collisionCooldown = 0.5f; // half-second cooldown before next shake/sound
            }
        }
        */
        if (playerKart.isBoosting()) {
            camera.addShake(0.05f, 0.05f); // barely noticeable continuous shake while boosting
            
            // Emit boost flames
            float backX = playerKart.x - MathUtils.sin(playerKart.angle) * 10f;
            float backZ = playerKart.y - MathUtils.cos(playerKart.angle) * 10f;
            particleSystem.spawnParticle(
                backX + MathUtils.random(-2f, 2f), playerKart.worldY + 2f, backZ + MathUtils.random(-2f, 2f),
                MathUtils.random(-3f, 3f), MathUtils.random(2f, 10f), MathUtils.random(-3f, 3f),
                MathUtils.random(1.5f, 3f), 1f, MathUtils.random(0.3f, 0.7f), 0.1f, 0.8f, 0.3f
            );
        }
        
        // Dirt particles when off-track
        if (playerKart.getSurface() == TrackSurface.OFFTRACK && Math.abs(playerKart.getSpeed()) > 10f) {
            float backX = playerKart.x - MathUtils.sin(playerKart.angle) * 8f;
            float backZ = playerKart.y - MathUtils.cos(playerKart.angle) * 8f;
            particleSystem.spawnParticle(
                backX + MathUtils.random(-3f, 3f), playerKart.worldY + 0.5f, backZ + MathUtils.random(-3f, 3f),
                MathUtils.random(-10f, 10f), MathUtils.random(5f, 15f), MathUtils.random(-10f, 10f),
                MathUtils.random(2f, 4f), 0.5f, 0.35f, 0.2f, 0.7f, 0.6f
            );
        }
        
        particleSystem.update(dt);

        // Feature 9: engine sound
        soundManager.update(playerKart.getSpeed(), true,
                            playerKart.isDrifting(), playerKart.isBoosting());

        camera.follow(playerKart.x, playerKart.worldY, playerKart.y,
                      playerKart.angle, dt);
    }

    // ── Feature 6: Skid stamp ────────────────────────────────────────

    private void stampSkid(float wx, float wz) {
        if (skidCount < MAX_SKID_MARKS) {
            skidX[skidCount] = wx;
            skidZ[skidCount] = wz;
            skidA[skidCount] = 0.85f;
            skidCount++;
        } else {
            // Ring-buffer: overwrite oldest
            System.arraycopy(skidX, 1, skidX, 0, MAX_SKID_MARKS - 1);
            System.arraycopy(skidZ, 1, skidZ, 0, MAX_SKID_MARKS - 1);
            System.arraycopy(skidA, 1, skidA, 0, MAX_SKID_MARKS - 1);
            skidX[MAX_SKID_MARKS - 1] = wx;
            skidZ[MAX_SKID_MARKS - 1] = wz;
            skidA[MAX_SKID_MARKS - 1] = 0.85f;
        }
    }

    // ── Feature 2: Restart ────────────────────────────────────────────

    private void restart() {
        trackRenderer.dispose();
        hud.dispose();
        if (finishFlagTex != null) finishFlagTex.dispose();
        loadBestTime();
        init();
    }

    // ── Screen interface ──────────────────────────────────────────────

    @Override public void resize(int w, int h) {
        orthoCamera.setToOrtho(false, w, h);
        camera.cam.viewportWidth  = w;
        camera.cam.viewportHeight = h;
        camera.cam.update();
    }
    @Override public void show()   {}
    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}
    @Override public void dispose() {
        trackRenderer.dispose();
        hud.dispose();
        particleSystem.dispose();
        if (finishFlagTex  != null) finishFlagTex.dispose();
        if (soundManager   != null) soundManager.dispose();
    }
}
