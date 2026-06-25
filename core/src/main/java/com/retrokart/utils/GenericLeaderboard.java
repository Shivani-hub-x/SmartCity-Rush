package com.retrokart.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.Iterator;

/**
 * GenericLeaderboard&lt;T extends Comparable&lt;T&gt;&gt; – type-safe score board.
 *
 * Syllabus coverage (TCS-408):
 *  Unit 4 – Generics:
 *    • Generic class with bounded type parameter (T extends Comparable<T>)
 *    • Generic methods
 *    • Type safety: compiler enforces correct entry type
 *    • TreeSet: sorted, no duplicates (Unit 4: Collection classes)
 *    • Iterator, ListIterator: traversal of results (Unit 4)
 *
 * Usage example:
 *   GenericLeaderboard<Float> board = new GenericLeaderboard<>(10, true);
 *   board.addEntry(42.73f, "Alice");
 *   board.addEntry(38.10f, "Bob");
 *   List<GenericLeaderboard.Entry<Float>> top = board.getTopEntries(5);
 */
public class GenericLeaderboard<T extends Comparable<T>> {

    // ── Inner generic Entry class ─────────────────────────────────────
    public static class Entry<T extends Comparable<T>> implements Comparable<Entry<T>> {
        public final T      score;
        public final String playerName;
        public final long   timestamp;

        public Entry(T score, String playerName) {
            this.score      = score;
            this.playerName = playerName;
            this.timestamp  = System.currentTimeMillis();
        }

        @Override
        public int compareTo(Entry<T> other) {
            // Primary: score ascending (lower time = better for race)
            int cmp = this.score.compareTo(other.score);
            if (cmp != 0) return cmp;
            // Tie-break: earlier timestamp wins
            return Long.compare(this.timestamp, other.timestamp);
        }

        @Override
        public String toString() {
            return String.format("Entry{player='%s', score=%s}", playerName, score);
        }
    }

    // ── Fields ────────────────────────────────────────────────────────
    private final int          maxSize;
    private final boolean      ascending;   // true = lower score is better (race times)
    private final TreeSet<Entry<T>> board;   // TreeSet: auto-sorted, no duplicates

    // ── Constructor overloading (Unit 2) ──────────────────────────────

    /** Creates a leaderboard with default capacity of 10, ascending order. */
    public GenericLeaderboard() {
        this(10, true);
    }

    /** Creates a leaderboard with specified capacity and sort order. */
    public GenericLeaderboard(int maxSize, boolean ascending) {
        this.maxSize   = maxSize;
        this.ascending = ascending;
        // TreeSet with natural ordering (Entry implements Comparable)
        this.board = ascending
            ? new TreeSet<>()
            : new TreeSet<>(Comparator.reverseOrder());
    }

    // ── Generic methods ───────────────────────────────────────────────

    /**
     * Adds an entry to the board. If the board is full and the new entry
     * is better than the worst, the worst is evicted.
     *
     * @param score      the score value (e.g. finish time as Float)
     * @param playerName the player's display name
     * @return true if the entry made it onto the board
     */
    public boolean addEntry(T score, String playerName) {
        Entry<T> entry = new Entry<>(score, playerName);
        board.add(entry);
        if (board.size() > maxSize) {
            board.pollLast();   // remove worst entry
            return board.contains(entry);
        }
        return true;
    }

    /**
     * Returns up to {@code n} top entries as an ArrayList.
     * Demonstrates Iterator usage (Unit 4: Iterator).
     *
     * @param n maximum number of entries to return
     */
    public List<Entry<T>> getTopEntries(int n) {
        List<Entry<T>> result = new ArrayList<>();
        // Unit 4: Iterator traversal over TreeSet
        Iterator<Entry<T>> it = board.iterator();
        int count = 0;
        while (it.hasNext() && count < n) {
            result.add(it.next());
            count++;
        }
        return result;
    }

    /**
     * Static generic utility: returns the minimum entry from any list.
     * Demonstrates a standalone generic method (Unit 4: Generic Methods).
     *
     * @param entries non-empty list of Comparable entries
     * @param <U>     any Comparable type
     * @return the minimum entry according to natural ordering
     */
    public static <U extends Comparable<U>> U findMin(List<U> entries) {
        if (entries == null || entries.isEmpty())
            throw new IllegalArgumentException("List must not be empty");
        U min = entries.get(0);
        // Unit 4: ListIterator
        for (int i = 1; i < entries.size(); i++) {
            if (entries.get(i).compareTo(min) < 0) {
                min = entries.get(i);
            }
        }
        return min;
    }

    /** Returns the number of entries currently on the board. */
    public int size() { return board.size(); }

    /** Returns true if the board has no entries. */
    public boolean isEmpty() { return board.isEmpty(); }

    /** Clears all entries. */
    public void clear() { board.clear(); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GenericLeaderboard[\n");
        int rank = 1;
        for (Entry<T> e : board) {
            sb.append(String.format("  %d. %s -> %s%n", rank++, e.playerName, e.score));
        }
        sb.append("]");
        return sb.toString();
    }
}
