package com.retrokart.exception;

/**
 * RaceException – custom exception hierarchy for RetroKart.
 *
 * Syllabus coverage (TCS-408):
 *  Unit 3 – Exception Handling:
 *    • Defining Custom Exceptions (checked and unchecked variants)
 *    • Exception Hierarchy
 *    • throw statement / throws clause
 *    • try-catch-finally blocks (used in RaceValidator and DatabaseManager)
 *
 * Hierarchy:
 *   Throwable
 *     └── Exception
 *           └── RaceException          ← checked custom exception
 *                 ├── InvalidNameException
 *                 └── TimeLimitException
 *     └── RuntimeException
 *           └── RaceStateException     ← unchecked custom exception
 */
public class RaceException extends Exception {

    private final int errorCode;

    /** Standard checked race exception with a message and error code. */
    public RaceException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /** Wraps an underlying cause (used when re-throwing from JDBC code). */
    public RaceException(String message, int errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() { return errorCode; }

    // ── Checked subclasses ─────────────────────────────────────────────

    /**
     * Thrown when a player enters a name that is blank, too long, or
     * contains illegal characters.
     */
    public static class InvalidNameException extends RaceException {
        public InvalidNameException(String name) {
            super("Invalid player name: '" + name + "'. Must be 1-12 alphanumeric chars.", 1001);
        }
    }

    /**
     * Thrown when a finish time is outside the valid race window
     * (e.g. negative, or longer than the maximum race duration).
     */
    public static class TimeLimitException extends RaceException {
        private final float invalidTime;
        public TimeLimitException(float time, float maxTime) {
            super(String.format("Finish time %.2fs is outside valid range [0, %.2f].", time, maxTime), 1002);
            this.invalidTime = time;
        }
        public float getInvalidTime() { return invalidTime; }
    }

    // ── Unchecked subclass ─────────────────────────────────────────────

    /**
     * Thrown when an operation is attempted in an illegal race state
     * (e.g. calling saveResult before the race has started).
     * Unchecked — extends RuntimeException.
     */
    public static class RaceStateException extends RuntimeException {
        public RaceStateException(String message) {
            super("[RaceStateException] " + message);
        }
    }
}
