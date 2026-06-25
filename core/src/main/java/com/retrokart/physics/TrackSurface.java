package com.retrokart.physics;

public enum TrackSurface {
    ROAD, GRASS, OFFTRACK;

    /** Classify by whether terrain was found and its height. */
    public static TrackSurface fromHeight(float y, boolean hit) {
        if (!hit) return OFFTRACK;
        if (y > -30f && y < 30f) return ROAD;
        return GRASS;
    }
}
