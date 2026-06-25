package com.retrokart.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseManager
 *
 * Thin JDBC/SQLite wrapper for RetroKart.
 *
 * Schema
 * ──────
 *   race_results(id INTEGER PK, player_name TEXT, finish_time REAL, race_date TEXT)
 *
 * Usage
 * ──────
 *   DatabaseManager db = DatabaseManager.getInstance();
 *   db.saveResult("Player1", 42.7f);
 *   List<RaceResult> top = db.getTopResults(10);
 *   db.close();
 *
 * The SQLite database file is written next to the game executable as
 * "retrokart_scores.db".  On first run the schema is created automatically.
 */
public class DatabaseManager {

    // ── Singleton ─────────────────────────────────────────────────────
    private static DatabaseManager instance;

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // ── JDBC state ────────────────────────────────────────────────────
    private Connection connection;
    private static final String DB_FILE = "retrokart_scores.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_FILE;

    // ── Constructor ───────────────────────────────────────────────────
    private DatabaseManager() {
        try {
            // Load the SQLite JDBC driver explicitly (needed on some JVMs)
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(JDBC_URL);
            createSchema();
            System.out.println("[RetroKart DB] Connected to " + DB_FILE);
        } catch (ClassNotFoundException e) {
            System.err.println("[RetroKart DB] SQLite JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[RetroKart DB] Connection error: " + e.getMessage());
        }
    }

    // ── Schema ────────────────────────────────────────────────────────

    /**
     * Creates the race_results table if it does not already exist.
     * Safe to call on every launch.
     */
    private void createSchema() throws SQLException {
        String sql =
            "CREATE TABLE IF NOT EXISTS race_results (" +
            "  id          INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  player_name TEXT    NOT NULL DEFAULT 'Player'," +
            "  finish_time REAL    NOT NULL," +           // seconds (lower = better)
            "  race_date   TEXT    NOT NULL" +            // ISO-8601 timestamp
            ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    // ── Write ─────────────────────────────────────────────────────────

    /**
     * Saves a finished race result to the database.
     *
     * @param playerName  Display name shown on the leaderboard
     * @param finishTime  Race completion time in seconds (lower = faster)
     * @return            The auto-generated row ID, or -1 on failure
     */
    public long saveResult(String playerName, float finishTime) {
        if (connection == null) return -1;

        String sql = "INSERT INTO race_results (player_name, finish_time, race_date) VALUES (?, ?, datetime('now'))";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, playerName == null || playerName.isEmpty() ? "Player" : playerName);
            ps.setFloat(2, finishTime);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    System.out.printf("[RetroKart DB] Saved result – id=%d  name=%s  time=%.2fs%n",
                                      id, playerName, finishTime);
                    return id;
                }
            }
        } catch (SQLException e) {
            System.err.println("[RetroKart DB] saveResult error: " + e.getMessage());
        }
        return -1;
    }

    // ── Read ──────────────────────────────────────────────────────────

    /**
     * Returns the top N results ordered by fastest finish time.
     *
     * @param limit  Maximum number of rows to return
     */
    public List<RaceResult> getTopResults(int limit) {
        List<RaceResult> results = new ArrayList<>();
        if (connection == null) return results;

        // GROUP BY player_name so each player appears only once with their best time
        String sql = "SELECT player_name, MIN(finish_time) AS finish_time, " +
                     "MAX(race_date) AS race_date FROM race_results " +
                     "GROUP BY player_name ORDER BY finish_time ASC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    results.add(new RaceResult(
                        rank++,
                        rs.getString("player_name"),
                        rs.getFloat("finish_time"),
                        rs.getString("race_date")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RetroKart DB] getTopResults error: " + e.getMessage());
        }
        return results;
    }

    /**
     * Returns the single best (fastest) time ever recorded.
     * Returns 0f if no records exist.
     */
    public float getPersonalBest(String playerName) {
        if (connection == null) return 0f;

        String sql = "SELECT MIN(finish_time) AS best FROM race_results WHERE player_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    float best = rs.getFloat("best");
                    return rs.wasNull() ? 0f : best;
                }
            }
        } catch (SQLException e) {
            System.err.println("[RetroKart DB] getPersonalBest error: " + e.getMessage());
        }
        return 0f;
    }

    /**
     * Returns the rank of the player's best time (1 = fastest ever).
     * Returns -1 if the player has no recorded results.
     */
    public int getPlayerBestRank(String playerName) {
        if (connection == null) return -1;
        String sql = "SELECT COUNT(*) + 1 AS rank FROM race_results " +
                     "WHERE finish_time < (SELECT MIN(finish_time) FROM race_results WHERE player_name = ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int r = rs.getInt("rank");
                    return rs.wasNull() ? -1 : r;
                }
            }
        } catch (SQLException e) {
            System.err.println("[RetroKart DB] getPlayerBestRank error: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Returns the total number of completed races stored.
     */
    public int getTotalRaces() {
        if (connection == null) return 0;
        String sql = "SELECT COUNT(*) FROM race_results";
        try (Statement stmt = connection.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[RetroKart DB] getTotalRaces error: " + e.getMessage());
        }
        return 0;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────

    /**
     * Closes the JDBC connection.  Call this when the game exits.
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("[RetroKart DB] Connection closed.");
            } catch (SQLException e) {
                System.err.println("[RetroKart DB] close error: " + e.getMessage());
            }
        }
    }

    /**
     * @return true if the JDBC connection is open and healthy.
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // ── Inner DTO ─────────────────────────────────────────────────────

    /**
     * Immutable data transfer object for a single leaderboard entry.
     */
    public static class RaceResult {
        public final int    rank;
        public final String playerName;
        public final float  finishTime;   // seconds
        public final String raceDate;     // ISO-8601

        public RaceResult(int rank, String playerName, float finishTime, String raceDate) {
            this.rank        = rank;
            this.playerName  = playerName;
            this.finishTime  = finishTime;
            this.raceDate    = raceDate;
        }

        /** Formats finish time as M:SS.ss */
        public String formattedTime() {
            int minutes = (int)(finishTime / 60f);
            float seconds = finishTime % 60f;
            return String.format("%d:%05.2f", minutes, seconds);
        }

        @Override
        public String toString() {
            return String.format("#%d  %-12s  %s  (%s)",
                rank, playerName, formattedTime(), raceDate);
        }
    }
}
