package com.retrokart;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.retrokart.db.DatabaseManager;
import com.retrokart.screens.MenuScreen;
import com.retrokart.utils.AssetFactory;

/**
 * RetroKartGame - application entry point.
 *
 * Feature 8: Now opens MenuScreen first instead of going straight to race.
 */
public class RetroKartGame extends Game {

    public SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();
        AssetFactory.init();
        DatabaseManager.getInstance();       // JDBC: open DB connection at startup
        setScreen(new MenuScreen(this));      // Feature 8: start at menu
    }

    @Override
    public void dispose() {
        batch.dispose();
        AssetFactory.dispose();
        DatabaseManager.getInstance().close(); // JDBC: flush & close SQLite connection
        super.dispose();
    }
}
