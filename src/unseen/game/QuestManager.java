package unseen.game;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Tracks two related systems:
 *
 * 1. Round quests:
 *    - One active quest per floor.
 *    - Progress resets when a new floor starts.
 *    - Completing it grants an immediate item reward through GamePanel.
 *
 * 2. Achievements:
 *    - Permanent unlocks.
 *    - Saved to disk so they remain unlocked after closing the game.
 */
public class QuestManager {

    public enum QuestEvent {
        TURN,
        KILL,
        PICKUP,
        FLOOR_CLEAR
    }

    public static class Quest {
        private final String name;
        private final String description;
        private final QuestEvent event;
        private final int target;
        private int progress;
        private boolean completedThisRound;

        Quest(String name, String description, QuestEvent event, int target) {
            this.name = name;
            this.description = description;
            this.event = event;
            this.target = Math.max(1, target);
        }

        void resetForRound() {
            progress = 0;
            completedThisRound = false;
        }

        boolean advance(QuestEvent incomingEvent, int amount) {
            if (completedThisRound || incomingEvent != event) {
                return false;
            }

            progress = Math.min(target, progress + Math.max(1, amount));
            if (progress >= target) {
                completedThisRound = true;
                return true;
            }
            return false;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public QuestEvent getEvent() {
            return event;
        }

        public int getTarget() {
            return target;
        }

        public int getProgress() {
            return progress;
        }

        public boolean isCompletedThisRound() {
            return completedThisRound;
        }
    }

    public static class Achievement {
        private final String id;
        private final String name;
        private final String description;
        private boolean unlocked;

        Achievement(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean isUnlocked() {
            return unlocked;
        }

        void unlock() {
            unlocked = true;
        }
    }

    private static final String SAVE_FILE = "unseen_achievements.dat";

    private final List<Quest> roundQuests = new ArrayList<>();
    private final List<Achievement> achievements = new ArrayList<>();
    private final List<String> pendingAchievementToasts = new ArrayList<>();
    private final Random random = new Random();

    private Quest activeQuest;

    private int runTurns;
    private int runKills;
    private int runPickups;
    private int runFloorsCleared;
    private int runFakeLadders;

    private int floorTurns;
    private int floorKills;
    private int floorPickups;
    private boolean hitThisFloor;

    public QuestManager() {
        buildRoundQuests();
        buildAchievements();
        loadAchievements();
        resetRun();
    }

    private void buildRoundQuests() {
        roundQuests.add(new Quest(
                "First Blood",
                "Defeat 1 enemy on this floor.",
                QuestEvent.KILL,
                1));
        roundQuests.add(new Quest(
                "Clean Sweep",
                "Defeat 2 enemies on this floor.",
                QuestEvent.KILL,
                2));
        roundQuests.add(new Quest(
                "Scavenger",
                "Pick up 1 item on this floor.",
                QuestEvent.PICKUP,
                1));
        roundQuests.add(new Quest(
                "Pack Rat",
                "Pick up 2 items on this floor.",
                QuestEvent.PICKUP,
                2));
        roundQuests.add(new Quest(
                "Keep Moving",
                "Survive 6 turns on this floor.",
                QuestEvent.TURN,
                6));
        roundQuests.add(new Quest(
                "Steady Nerves",
                "Survive 12 turns on this floor.",
                QuestEvent.TURN,
                12));
        roundQuests.add(new Quest(
                "Find the Way",
                "Reach the real ladder.",
                QuestEvent.FLOOR_CLEAR,
                1));
    }

    private void buildAchievements() {
        achievements.add(new Achievement(
                "first_step",
                "First Step",
                "Survive your first turn."));
        achievements.add(new Achievement(
                "first_blood",
                "First Blood",
                "Defeat your first enemy."));
        achievements.add(new Achievement(
                "hunter",
                "Hunter",
                "Defeat 5 enemies in one run."));
        achievements.add(new Achievement(
                "executioner",
                "Executioner",
                "Defeat 10 enemies in one run."));
        achievements.add(new Achievement(
                "scavenger",
                "Scavenger",
                "Pick up your first item."));
        achievements.add(new Achievement(
                "collector",
                "Collector",
                "Pick up 5 items in one run."));
        achievements.add(new Achievement(
                "first_descent",
                "First Descent",
                "Clear floor 1."));
        achievements.add(new Achievement(
                "deep_diver",
                "Deep Diver",
                "Reach floor 4."));
        achievements.add(new Achievement(
                "abyss_walker",
                "Abyss Walker",
                "Reach floor 6."));
        achievements.add(new Achievement(
                "ghost_step",
                "Ghost Step",
                "Clear a floor without taking damage."));
        achievements.add(new Achievement(
                "patient",
                "Patient",
                "Survive 20 turns on a single floor."));
        achievements.add(new Achievement(
                "deceived",
                "Deceived",
                "Step onto a fake ladder."));
        achievements.add(new Achievement(
                "horror_survivor",
                "Horror Survivor",
                "Clear a floor in Horror Mode."));
    }

    public void resetRun() {
        runTurns = 0;
        runKills = 0;
        runPickups = 0;
        runFloorsCleared = 0;
        runFakeLadders = 0;
        startQuestForFloor(1);
    }

    public void startQuestForFloor(int floorNumber) {
        floorTurns = 0;
        floorKills = 0;
        floorPickups = 0;
        hitThisFloor = false;

        for (Quest quest : roundQuests) {
            quest.resetForRound();
        }

        activeQuest = roundQuests.get(random.nextInt(roundQuests.size()));
        activeQuest.resetForRound();
    }

    public boolean recordTurn() {
        runTurns++;
        floorTurns++;

        unlockIfNeeded("first_step");
        if (floorTurns >= 20) {
            unlockIfNeeded("patient");
        }

        return advanceRoundQuest(QuestEvent.TURN, 1);
    }

    public boolean recordKills(int kills) {
        if (kills <= 0) {
            return false;
        }

        runKills += kills;
        floorKills += kills;

        if (runKills >= 1) {
            unlockIfNeeded("first_blood");
        }
        if (runKills >= 5) {
            unlockIfNeeded("hunter");
        }
        if (runKills >= 10) {
            unlockIfNeeded("executioner");
        }

        return advanceRoundQuest(QuestEvent.KILL, kills);
    }

    public boolean recordPickup() {
        runPickups++;
        floorPickups++;

        if (runPickups >= 1) {
            unlockIfNeeded("scavenger");
        }
        if (runPickups >= 5) {
            unlockIfNeeded("collector");
        }

        return advanceRoundQuest(QuestEvent.PICKUP, 1);
    }

    public void recordHit() {
        hitThisFloor = true;
    }

    public void recordFakeExit() {
        runFakeLadders++;
        unlockIfNeeded("deceived");
    }

    public boolean recordFloorCleared(int floorNumber, boolean horrorMode) {
        runFloorsCleared++;

        if (floorNumber >= 1) {
            unlockIfNeeded("first_descent");
        }
        if (floorNumber >= 3) {
            unlockIfNeeded("deep_diver");
        }
        if (floorNumber >= 5) {
            unlockIfNeeded("abyss_walker");
        }
        if (!hitThisFloor) {
            unlockIfNeeded("ghost_step");
        }
        if (horrorMode) {
            unlockIfNeeded("horror_survivor");
        }

        return advanceRoundQuest(QuestEvent.FLOOR_CLEAR, 1);
    }

    private boolean advanceRoundQuest(QuestEvent event, int amount) {
        return activeQuest != null && activeQuest.advance(event, amount);
    }

    private void unlockIfNeeded(String id) {
        Achievement achievement = findAchievement(id);
        if (achievement == null || achievement.isUnlocked()) {
            return;
        }

        achievement.unlock();
        pendingAchievementToasts.add("Achievement unlocked: " + achievement.getName());
        saveAchievements();
    }

    private Achievement findAchievement(String id) {
        for (Achievement achievement : achievements) {
            if (achievement.getId().equals(id)) {
                return achievement;
            }
        }
        return null;
    }

    private Path savePath() {
        return Paths.get(System.getProperty("user.home"), SAVE_FILE);
    }

    private void loadAchievements() {
        Path path = savePath();
        if (!Files.exists(path)) {
            return;
        }

        Set<String> unlockedIds = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String id = line.trim();
                if (!id.isEmpty()) {
                    unlockedIds.add(id);
                }
            }
        } catch (IOException ex) {
            System.out.println("Could not load achievements: " + ex.getMessage());
            return;
        }

        for (Achievement achievement : achievements) {
            if (unlockedIds.contains(achievement.getId())) {
                achievement.unlock();
            }
        }
    }

    private void saveAchievements() {
        Path path = savePath();
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (Achievement achievement : achievements) {
                if (achievement.isUnlocked()) {
                    writer.write(achievement.getId());
                    writer.newLine();
                }
            }
        } catch (IOException ex) {
            System.out.println("Could not save achievements: " + ex.getMessage());
        }
    }

    public List<String> drainAchievementToasts() {
        List<String> copy = new ArrayList<>(pendingAchievementToasts);
        pendingAchievementToasts.clear();
        return copy;
    }

    public Quest getActiveQuest() {
        return activeQuest;
    }

    public List<Quest> getRoundQuests() {
        return Collections.unmodifiableList(roundQuests);
    }

    public List<Achievement> getAchievements() {
        return Collections.unmodifiableList(achievements);
    }

    public List<Achievement> getCompletedAchievements() {
        List<Achievement> completed = new ArrayList<>();
        for (Achievement achievement : achievements) {
            if (achievement.isUnlocked()) {
                completed.add(achievement);
            }
        }
        return completed;
    }

    public List<Achievement> getLockedAchievements() {
        List<Achievement> locked = new ArrayList<>();
        for (Achievement achievement : achievements) {
            if (!achievement.isUnlocked()) {
                locked.add(achievement);
            }
        }
        return locked;
    }

    public int getRunTurns() {
        return runTurns;
    }

    public int getRunKills() {
        return runKills;
    }

    public int getRunPickups() {
        return runPickups;
    }

    public int getRunFloorsCleared() {
        return runFloorsCleared;
    }

    public int getRunFakeLadders() {
        return runFakeLadders;
    }
}
