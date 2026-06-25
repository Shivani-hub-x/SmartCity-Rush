package com.retrokart.entities;

/**
 * GameEntity – abstract base for all game world objects.
 *
 * Syllabus coverage (TCS-408):
 *  Unit 2 – Abstract Class: defines abstract update/describe methods.
 *           Final keyword: ID field is immutable once assigned.
 *           Static variable: shared entity counter.
 *           Encapsulation: protected fields, public accessors.
 *
 * All concrete entities (Kart, BotKart, ItemBox) indirectly extend this
 * through Entity, establishing the full OOP inheritance chain:
 *   GameEntity  (abstract)
 *       └── Entity        (concrete rendering base)
 *               ├── Kart
 *               ├── BotKart
 *               └── ItemBox
 */
public abstract class GameEntity {

    // ── Static counter (Unit 2: static variable) ──────────────────────
    private static int entityCounter = 0;

    /**
     * Returns the total number of GameEntity instances created this session.
     * Demonstrates static method: accessible without an instance.
     */
    public static int getTotalEntityCount() {
        return entityCounter;
    }

    // ── Final field (Unit 2: final keyword on variable) ───────────────
    /** Unique, immutable entity ID assigned at construction time. */
    public final int entityId;

    // ── Encapsulated state (Unit 2: encapsulation) ────────────────────
    private boolean alive = true;
    private String  tag;

    // ── Constructor (Unit 2: constructor with initialisation) ─────────
    protected GameEntity(String tag) {
        this.entityId = ++entityCounter;
        this.tag      = tag;
    }

    // ── Abstract methods (Unit 2: abstract methods) ───────────────────

    /**
     * Update this entity's state by {@code dt} seconds.
     * Each concrete subclass must define its own behaviour.
     */
    public abstract void update(float dt);

    /**
     * Returns a human-readable description of this entity's current state.
     * Used in debug overlays and the console log.
     */
    public abstract String describe();

    // ── Concrete methods ──────────────────────────────────────────────

    public boolean isAlive()      { return alive; }
    public void    setAlive(boolean v) { alive = v; }

    public String  getTag()       { return tag; }
    public void    setTag(String t){ this.tag = t; }

    @Override
    public String toString() {
        return String.format("[%s #%d alive=%b]", tag, entityId, alive);
    }
}
