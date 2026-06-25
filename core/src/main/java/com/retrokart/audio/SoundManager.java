package com.retrokart.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Feature 9 - SoundManager.
 *
 * All audio is generated procedurally from PCM math - no asset files needed.
 *
 * Sounds:
 *  - Engine hum  : continuous rumble, pitch tracks speed
 *  - Drift screech: noisy burst on drift start
 *  - Boost whoosh : rising tone on boost
 *  - Finish fanfare: 4-note ascending arpeggio on win
 *  - Countdown beep: low beep for 3/2/1, high beep for GO
 */
public class SoundManager implements Disposable {

    private static final int SAMPLE_RATE = 22050;

    // Engine hum - runs on a background thread writing to AudioDevice
    private Thread         engineThread;
    private volatile boolean engineRunning = false;
    private volatile float   enginePitch   = 0.3f;   // 0..1
    private volatile boolean engineActive  = false;
    private AudioDevice    engineDevice;

    // One-shot sounds
    private Sound driftSound;
    private Sound boostSound;
    private Sound finishSound;
    private Sound countBeep;
    private Sound goBeep;

    // Drift state for trigger-once logic
    private boolean wasDrifting = false;
    private boolean wasBoosting = false;

    public SoundManager() {
        buildOneShots();
        startEngineThread();
    }

    // ── Engine thread ─────────────────────────────────────────────────

    private void startEngineThread() {
        engineRunning = true;
        engineDevice  = Gdx.audio.newAudioDevice(SAMPLE_RATE, true);
        engineThread  = new Thread(() -> {
            float phase = 0f;
            short[] buf = new short[512];
            while (engineRunning) {
                if (!engineActive) {
                    // Write silence so device stays happy
                    java.util.Arrays.fill(buf, (short) 0);
                    engineDevice.writeSamples(buf, 0, buf.length);
                    try { Thread.sleep(8); } catch (InterruptedException ignored) {}
                    continue;
                }
                float freq     = 55f + enginePitch * 130f;
                float phaseInc = (float)(2.0 * Math.PI * freq / SAMPLE_RATE);
                for (int i = 0; i < buf.length; i++) {
                    float s = 0.40f * (float) Math.sin(phase)
                            + 0.22f * (float) Math.sin(phase * 2f)
                            + 0.10f * (float) Math.sin(phase * 3f);
                    buf[i] = (short)(s * Short.MAX_VALUE * 0.50f);
                    phase += phaseInc;
                    if (phase > Math.PI * 2) phase -= (float)(Math.PI * 2);
                }
                engineDevice.writeSamples(buf, 0, buf.length);
            }
        }, "retrokart-engine");
        engineThread.setDaemon(true);
        engineThread.start();
    }

    /**
     * Call every frame. Handles engine pitch, drift screech trigger, boost trigger.
     *
     * @param speed      kart speed (0..80)
     * @param raceActive true if countdown is done and race is not finished
     * @param drifting   kart.isDrifting()
     * @param boosting   kart.isBoosting()
     */
    public void update(float speed, boolean raceActive,
                       boolean drifting, boolean boosting) {
        engineActive = raceActive;
        enginePitch  = Math.min(Math.abs(speed) / 80f, 1f);

        // Drift screech - play once per drift entry
        if (drifting && !wasDrifting) playDrift();
        wasDrifting = drifting;

        // Boost whoosh - play once per boost entry
        if (boosting && !wasBoosting) playBoost();
        wasBoosting = boosting;
    }

    // ── One-shot builders ─────────────────────────────────────────────

    private void buildOneShots() {
        driftSound  = wavToSound(buildDrift());
        boostSound  = wavToSound(buildBoost());
        finishSound = wavToSound(buildFinish());
        countBeep   = wavToSound(buildBeep(440f, 0.09f));
        goBeep      = wavToSound(buildBeep(880f, 0.20f));
    }

    // ── PCM math ─────────────────────────────────────────────────────

    private short[] buildBeep(float freq, float dur) {
        int n = (int)(SAMPLE_RATE * dur);
        short[] s = new short[n];
        double ph = 0, inc = 2.0 * Math.PI * freq / SAMPLE_RATE;
        for (int i = 0; i < n; i++) {
            float t   = (float) i / n;
            float env = t < 0.1f ? t / 0.1f : (t > 0.7f ? (1f - t) / 0.3f : 1f);
            s[i] = (short)(Math.sin(ph) * env * Short.MAX_VALUE * 0.55f);
            ph  += inc;
        }
        return s;
    }

    private short[] buildDrift() {
        int n = (int)(SAMPLE_RATE * 0.30f);
        short[] s = new short[n];
        for (int i = 0; i < n; i++) {
            float t     = (float) i / n;
            float freq  = 1400f - t * 700f;
            float env   = (1f - t) * 0.55f;
            float noise = (float)(Math.random() * 2.0 - 1.0);
            float tone  = (float) Math.sin(2.0 * Math.PI * freq * i / SAMPLE_RATE);
            s[i] = (short)((noise * 0.65f + tone * 0.35f) * env * Short.MAX_VALUE);
        }
        return s;
    }

    private short[] buildBoost() {
        int n = (int)(SAMPLE_RATE * 0.25f);
        short[] s = new short[n];
        for (int i = 0; i < n; i++) {
            float t    = (float) i / n;
            float freq = 280f + t * 1000f;
            float env  = t < 0.25f ? t / 0.25f : 1f - (t - 0.25f) / 0.75f;
            s[i] = (short)(Math.sin(2.0 * Math.PI * freq * i / SAMPLE_RATE)
                           * env * Short.MAX_VALUE * 0.55f);
        }
        return s;
    }

    private short[] buildFinish() {
        // C E G C' ascending arpeggio
        float[] freqs   = { 523f, 659f, 784f, 1047f };
        int     noteLen = (int)(SAMPLE_RATE * 0.17f);
        short[] s       = new short[noteLen * freqs.length];
        for (int ni = 0; ni < freqs.length; ni++) {
            double ph  = 0;
            double inc = 2.0 * Math.PI * freqs[ni] / SAMPLE_RATE;
            for (int i = 0; i < noteLen; i++) {
                float t   = (float) i / noteLen;
                float env = t < 0.08f ? t / 0.08f : 1f - t;
                s[ni * noteLen + i] = (short)(Math.sin(ph) * env
                                              * Short.MAX_VALUE * 0.6f);
                ph += inc;
            }
        }
        return s;
    }

    // ── WAV encoder ───────────────────────────────────────────────────

    private byte[] toWav(short[] samples) {
        int dataLen = samples.length * 2;
        byte[] b    = new byte[44 + dataLen];
        b[0]='R'; b[1]='I'; b[2]='F'; b[3]='F';
        putInt(b,  4, 36 + dataLen);
        b[8]='W'; b[9]='A'; b[10]='V'; b[11]='E';
        b[12]='f'; b[13]='m'; b[14]='t'; b[15]=' ';
        putInt(b,   16, 16);
        putShort(b, 20, (short) 1);   // PCM
        putShort(b, 22, (short) 1);   // mono
        putInt(b,   24, SAMPLE_RATE);
        putInt(b,   28, SAMPLE_RATE * 2);
        putShort(b, 32, (short) 2);
        putShort(b, 34, (short) 16);
        b[36]='d'; b[37]='a'; b[38]='t'; b[39]='a';
        putInt(b, 40, dataLen);
        for (int i = 0; i < samples.length; i++) {
            b[44 + i*2]     = (byte)(samples[i] & 0xFF);
            b[44 + i*2 + 1] = (byte)((samples[i] >> 8) & 0xFF);
        }
        return b;
    }

    private void putInt(byte[] b, int off, int v) {
        b[off]=(byte)v; b[off+1]=(byte)(v>>8); b[off+2]=(byte)(v>>16); b[off+3]=(byte)(v>>24);
    }
    private void putShort(byte[] b, int off, short v) {
        b[off]=(byte)v; b[off+1]=(byte)(v>>8);
    }

    private Sound wavToSound(short[] samples) {
        final byte[] wav = toWav(samples);
        return Gdx.audio.newSound(new FileHandle("dummy.wav") {
            @Override public InputStream read() { return new ByteArrayInputStream(wav); }
            @Override public boolean exists()   { return true; }
            @Override public String  path()     { return "dummy.wav"; }
            @Override public String  name()     { return "dummy.wav"; }
            @Override public String  extension(){ return "wav"; }
            @Override public String  nameWithoutExtension(){ return "dummy"; }
        });
    }

    // ── Public one-shot triggers ──────────────────────────────────────

    public void playDrift()     { if (driftSound  != null) driftSound.play(0.70f); }
    public void playBoost()     { if (boostSound  != null) boostSound.play(0.80f); }
    public void playFinish()    { if (finishSound != null) finishSound.play(0.90f); }
    public void playCountBeep() { if (countBeep   != null) countBeep.play(0.55f);  }
    public void playGoBeep()    { if (goBeep      != null) goBeep.play(0.80f);     }

    // ── Dispose ───────────────────────────────────────────────────────

    @Override
    public void dispose() {
        engineRunning = false;
        engineActive  = false;
        if (engineThread != null) {
            engineThread.interrupt();
            try { engineThread.join(500); } catch (InterruptedException ignored) {}
        }
        try { if (engineDevice != null) engineDevice.dispose(); } catch (Exception ignored) {}
        if (driftSound   != null) driftSound.dispose();
        if (boostSound   != null) boostSound.dispose();
        if (finishSound  != null) finishSound.dispose();
        if (countBeep    != null) countBeep.dispose();
        if (goBeep       != null) goBeep.dispose();
    }
}
