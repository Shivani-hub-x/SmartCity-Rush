package com.retrokart.interfaces;

import java.util.Comparator;
import java.util.List;

/**
 * Driveable – interface for any entity that can accelerate, steer, and brake.
 *
 * Syllabus coverage (TCS-408):
 *  Unit 3 – Defining interfaces, implementing interfaces, interface references.
 *            Default method  : getSpeedLabel() with default implementation.
 *            Static method   : getTopSpeedComparator() returns a Comparator lambda.
 *            Lambda functions: used in getTopSpeedComparator() and sortBySpeed().
 */
public interface Driveable {

    // ── Abstract interface methods ────────────────────────────────────

    /** Returns the current speed of this entity (units/sec). */
    float getSpeed();

    /** Returns the current steering angle in radians. */
    float getSteerAngle();

    /** Returns true if this entity is currently in a drift state. */
    boolean isDrifting();

    /** Applies an acceleration impulse of {@code force} units/sec². */
    void accelerate(float force);

    /** Applies braking. */
    void brake(float force);

    // ── Default method (Unit 3: default method in interface) ─────────

    /**
     * Returns a human-readable speed label with colour category.
     * Implemented here as a default so all implementors get it for free.
     */
    default String getSpeedLabel() {
        float spd = Math.abs(getSpeed());
        if (spd < 10f)  return "IDLE";
        if (spd < 35f)  return "SLOW";
        if (spd < 60f)  return "FAST";
        return "MAX!";
    }

    // ── Static method (Unit 3: static method in interface) ────────────

    /**
     * Returns a Comparator that sorts Driveables by descending speed.
     * Demonstrates: static interface method + lambda expression.
     */
    static <T extends Driveable> Comparator<T> getTopSpeedComparator() {
        // Lambda expression: (a, b) -> compare b's speed to a's (descending)
        return (a, b) -> Float.compare(Math.abs(b.getSpeed()), Math.abs(a.getSpeed()));
    }

    /**
     * Sorts a list of Driveables in-place by descending speed.
     * Demonstrates: static interface method + lambda via method reference.
     *
     * @param driveables mutable list to sort
     */
    static <T extends Driveable> void sortBySpeed(List<T> driveables) {
        driveables.sort(getTopSpeedComparator());
    }
}
