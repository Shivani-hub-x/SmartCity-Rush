package com.retrokart;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("RetroKart Beach Race");
        config.setWindowedMode(960, 640);
        config.setForegroundFPS(60);
        config.setResizable(false);
        config.useVsync(true);
        // Standard OpenGL - ModelBatch works with GL20
        config.setBackBufferConfig(8,8,8,8,24,0,0);
        new Lwjgl3Application(new RetroKartGame(), config);
    }
}
