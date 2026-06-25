package com.retrokart.utils;

import com.retrokart.exception.RaceException;
import com.retrokart.exception.RaceException.InvalidNameException;
import com.retrokart.exception.RaceException.TimeLimitException;

/**
 * RaceValidator – validation utilities using custom exceptions.
 *
 * Syllabus coverage (TCS-408):
 *  Unit 1 – StringBuffer: mutable string building in formatTime().
 *            Wrapper classes: Integer.parseInt, Float usage, autoboxing.
 *  Unit 3 – throws clause, throw statement, try-catch-finally.
 *            Custom exceptions: InvalidNameException, TimeLimitException.
 */
public class RaceValidator {

    public static final float MAX_RACE_TIME = 90f;   // matches RACE_DURATION in RaceScreen
    public static final int   MAX_NAME_LEN  = 12;

    // ── Name validation ───────────────────────────────────────────────

    /**
     * Validates a player name.
     *
     * @throws InvalidNameException if name is null, blank, too long,
     *                              or contains non-alphanumeric characters
     */
    public static String validateName(String name) throws InvalidNameException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidNameException(name == null ? "null" : "");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LEN) {
            throw new InvalidNameException(trimmed);
        }
        // Autoboxing: store length as Integer wrapper (Unit 1)
        Integer len = trimmed.length();
        if (len == 0) throw new InvalidNameException(trimmed);

        for (char c : trimmed.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != ' ') {
                throw new InvalidNameException(trimmed);
            }
        }
        return trimmed;
    }

    // ── Time validation ───────────────────────────────────────────────

    /**
     * Validates that {@code time} is in (0, MAX_RACE_TIME].
     *
     * @throws TimeLimitException if time is out of range
     */
    public static float validateTime(float time) throws TimeLimitException {
        if (time <= 0f || time > MAX_RACE_TIME) {
            throw new TimeLimitException(time, MAX_RACE_TIME);
        }
        return time;
    }

    // ── StringBuffer: time formatting (Unit 1) ─────────────────────────

    /**
     * Formats a raw float time (seconds) as "M:SS.ss" using StringBuffer
     * to demonstrate mutable string construction (Unit 1 – StringBuffer).
     *
     * @param seconds raw time in seconds, e.g. 42.73f
     * @return formatted string, e.g. "0:42.73"
     */
    public static String formatTime(float seconds) {
        int minutes  = (int)(seconds / 60f);
        float secs   = seconds % 60f;
        int secInt   = (int) secs;
        int centis   = (int)((secs - secInt) * 100f);

        // StringBuffer: mutable, thread-safe string building (Unit 1)
        StringBuffer sb = new StringBuffer();
        sb.append(minutes).append(":");
        if (secInt < 10) sb.append("0");
        sb.append(secInt).append(".");
        if (centis < 10) sb.append("0");
        sb.append(centis);
        return sb.toString();
    }

    // ── Wrapper class: parse time from string (Unit 1: Wrapper Classes) ──

    /**
     * Parses a time string "seconds.centiseconds" into a float.
     * Demonstrates Integer.parseInt and Float.parseFloat (Wrapper classes).
     *
     * @param timeStr e.g. "42.73"
     * @return float time value
     */
    public static float parseTimeString(String timeStr) {
        try {
            // Wrapper class usage: Float.parseFloat (autoboxes to Float)
            Float parsed = Float.parseFloat(timeStr);
            return parsed;  // unboxing
        } catch (NumberFormatException e) {
            // Custom exception re-throw (Unit 3: throw statement)
            throw new RaceException.RaceStateException(
                "Cannot parse time string: '" + timeStr + "'"
            );
        }
    }

    // ── Autoboxing demo (Unit 1) ───────────────────────────────────────

    /**
     * Returns the rank suffix ("st", "nd", "rd", "th") for a given rank.
     * Uses Integer wrapper via autoboxing for the rank parameter.
     */
    public static String rankSuffix(int rank) {
        Integer r = rank;  // autoboxing: int → Integer
        switch (r % 10) {
            case 1: return (r % 100 != 11) ? "st" : "th";
            case 2: return (r % 100 != 12) ? "nd" : "th";
            case 3: return (r % 100 != 13) ? "rd" : "th";
            default: return "th";
        }
    }
}
