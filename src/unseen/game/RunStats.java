package unseen.game;

import java.io.*;
import java.nio.file.*;

/**
 * Tracks per-run statistics and persists the all-time high score to a file.
 */
public class RunStats {

    private int turnsSurvived;
    private int enemiesKilled;
    private int floorsCleared;

    // Cached high score -- loaded once, updated on death/win
    private int highScoreFloors;
    private int highScoreTurns;
    private int highScoreKills;

    private static final String SAVE_FILE = "unseen_highscore.dat";

    public RunStats() {
        loadHighScore();
    }

    // -- Mutators ------------------------------------------------------

    public void incrementTurns()         { turnsSurvived++; }
    public void incrementKills()         { enemiesKilled++; }
    public void incrementKills(int n)    { enemiesKilled += n; }
    public void setFloorsCleared(int n)  { floorsCleared = n; }

    /** Resets the current-run counters (NOT the high score). */
    public void resetRun() {
        turnsSurvived = 0;
        enemiesKilled = 0;
        floorsCleared = 0;
    }

    // -- Accessors -----------------------------------------------------

    public int getTurnsSurvived()  { return turnsSurvived; }
    public int getEnemiesKilled()  { return enemiesKilled; }
    public int getFloorsCleared()  { return floorsCleared; }

    public int getHighScoreFloors() { return highScoreFloors; }
    public int getHighScoreTurns()  { return highScoreTurns; }
    public int getHighScoreKills()  { return highScoreKills; }

    public boolean isNewHighScore() {
        if (floorsCleared > highScoreFloors) return true;
        if (floorsCleared == highScoreFloors && turnsSurvived > highScoreTurns) return true;
        return false;
    }

    // -- Persistence ---------------------------------------------------

    /** Checks if the current run beats the record and saves if so. */
    public void commitHighScore() {
        if (isNewHighScore()) {
            highScoreFloors = floorsCleared;
            highScoreTurns  = turnsSurvived;
            highScoreKills  = enemiesKilled;
            saveHighScore();
        }
    }

    private void loadHighScore() {
        try {
            Path p = Paths.get(System.getProperty("user.home"), SAVE_FILE);
            if (!Files.exists(p)) return;
            String content = new String(Files.readAllBytes(p)).trim();
            String[] parts = content.split(",");
            if (parts.length >= 3) {
                highScoreFloors = Integer.parseInt(parts[0]);
                highScoreTurns  = Integer.parseInt(parts[1]);
                highScoreKills  = Integer.parseInt(parts[2]);
            }
        } catch (Exception ignored) { /* first run or corrupt file -- start at 0 */ }
    }

    private void saveHighScore() {
        try {
            Path p = Paths.get(System.getProperty("user.home"), SAVE_FILE);
            String data = highScoreFloors + "," + highScoreTurns + "," + highScoreKills;
            Files.write(p, data.getBytes());
        } catch (Exception ignored) { /* non-critical -- silently skip */ }
    }
}
