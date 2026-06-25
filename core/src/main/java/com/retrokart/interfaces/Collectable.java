package com.retrokart.interfaces;

/**
 * Collectable – interface for any world item that a kart can pick up.
 *
 * Syllabus coverage (TCS-408):
 *  Unit 3 – Defining and Implementing Interfaces.
 *            Interface extension: Collectable extends Identifiable.
 *            Interface reference: ItemBox is used via Collectable reference
 *                                 in RaceScreen's generic collection list.
 *
 * Extended by ItemBox.
 * Interface reference example (in RaceScreen):
 *   List<Collectable> pickups = new ArrayList<>();
 *   pickups.add(new ItemBox(...));
 *   for (Collectable c : pickups) { if (!c.isCollected()) c.collect(); }
 */
public interface Collectable {

    /**
     * Marks this item as collected.
     * After calling this, {@link #isCollected()} must return {@code true}.
     */
    void collect();

    /** Returns true if this item has already been collected. */
    boolean isCollected();

    /**
     * Returns a short name/type label for this collectable,
     * e.g. "BoostBox", "Coin", "Banana".
     */
    String getItemType();

    // ── Default method ────────────────────────────────────────────────

    /**
     * Returns a pickup status string suitable for the HUD or debug log.
     * Default implementation — subclasses may override.
     */
    default String getPickupStatus() {
        return getItemType() + (isCollected() ? " [USED]" : " [READY]");
    }
}
