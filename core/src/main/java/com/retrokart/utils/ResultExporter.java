package com.retrokart.utils;

import com.retrokart.db.DatabaseManager.RaceResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * ResultExporter – file I/O and Collection framework showcase.
 *
 * Syllabus coverage (TCS-408):
 *  Unit 3 – I/O Streams:
 *    • Character streams: FileWriter, BufferedWriter, FileReader, BufferedReader
 *    • Writing race results to "retrokart_results.txt"
 *    • Reading them back line-by-line
 *
 *  Unit 4 – Collection & Generic Framework:
 *    • ArrayList   : primary mutable list of results
 *    • LinkedList  : queue of pending writes (FIFO)
 *    • HashSet     : deduplicate player names
 *    • Collections : sort, reverse, unmodifiableList
 *    • Generics    : typed containers throughout
 */
public class ResultExporter {

    private static final String EXPORT_FILE = "retrokart_results.txt";

    // ── Unit 4: Generic ArrayList of results ─────────────────────────
    private final List<RaceResult> resultCache = new ArrayList<>();

    // ── Unit 4: LinkedList as a pending-write queue ───────────────────
    private final LinkedList<String> pendingLines = new LinkedList<>();

    // ── Unit 4: HashSet for unique player tracking ────────────────────
    private final Set<String> uniquePlayers = new HashSet<>();

    // ── Write (Unit 3: FileWriter + BufferedWriter) ───────────────────

    /**
     * Exports a list of RaceResults to the text file, appending a header.
     * Demonstrates: BufferedWriter wrapping FileWriter (character stream).
     *
     * @param results list of race results to export
     */
    public void exportToFile(List<RaceResult> results) {
        // Build pending lines into the LinkedList queue (Unit 4)
        pendingLines.clear();
        pendingLines.add("=== RetroKart Results Export ===");
        pendingLines.add(String.format("Total entries: %d", results.size()));
        pendingLines.add("Rank | Player       | Time   | Date");
        pendingLines.add("-----|--------------|--------|----------");
        for (RaceResult r : results) {
            String line = String.format("%-4d | %-12s | %-6s | %s",
                r.rank, r.playerName, r.formattedTime(),
                r.raceDate != null && r.raceDate.length() >= 10
                    ? r.raceDate.substring(0, 10) : "N/A");
            pendingLines.add(line);

            // HashSet: track unique player names (Unit 4)
            uniquePlayers.add(r.playerName);
        }
        pendingLines.add(String.format("Unique players: %d", uniquePlayers.size()));
        pendingLines.add("");  // blank footer line

        // Flush queue to file using BufferedWriter (Unit 3)
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(EXPORT_FILE, false))) {
            while (!pendingLines.isEmpty()) {
                // LinkedList.poll() removes from front (FIFO)
                String line = pendingLines.poll();
                bw.write(line);
                bw.newLine();
            }
            System.out.println("[ResultExporter] Written to " + EXPORT_FILE);
        } catch (IOException e) {
            System.err.println("[ResultExporter] Write error: " + e.getMessage());
        }

        // Cache the results (ArrayList – Unit 4)
        resultCache.clear();
        resultCache.addAll(results);
    }

    // ── Read (Unit 3: FileReader + BufferedReader) ────────────────────

    /**
     * Reads the export file back and returns all data lines (skipping header).
     * Demonstrates: BufferedReader.readLine() (character stream).
     *
     * @return unmodifiable list of non-blank lines (Unit 4: Collections.unmodifiableList)
     */
    public List<String> readFromFile() {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(EXPORT_FILE))) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (lineNum > 2 && !line.startsWith("---") && !line.isBlank()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("[ResultExporter] Read error: " + e.getMessage());
        }
        // Return an unmodifiable view (Collections utility – Unit 4)
        return Collections.unmodifiableList(lines);
    }

    // ── Collections operations (Unit 4) ──────────────────────────────

    /**
     * Returns the cached results sorted by finish time (ascending) via lambda.
     * Demonstrates: Collections.sort with Comparator lambda (Unit 4 + Unit 3 lambda).
     */
    public List<RaceResult> getSortedResults() {
        List<RaceResult> sorted = new ArrayList<>(resultCache);
        // Lambda comparator (Unit 3: Lambda Functions)
        sorted.sort((a, b) -> Float.compare(a.finishTime, b.finishTime));
        return sorted;
    }

    /**
     * Returns the set of unique player names seen this session.
     * Demonstrates: HashSet (Unit 4).
     */
    public Set<String> getUniquePlayers() {
        return Collections.unmodifiableSet(uniquePlayers);
    }

    /**
     * Returns the best (minimum) finish time from the cached results,
     * or 0f if no results are available.
     */
    public float getBestCachedTime() {
        if (resultCache.isEmpty()) return 0f;
        // Lambda + stream-like manual min (Unit 3: lambda, Unit 4: ArrayList iteration)
        return resultCache.stream()
            .map(r -> r.finishTime)
            .reduce(Float.MAX_VALUE, (a, b) -> a < b ? a : b);
    }
}
