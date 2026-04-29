package unseen.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
        private boolean completedEver;
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
            if (completedThisRound || incomingEvent != event) return false;

            progress = Math.min(target, progress + Math.max(1, amount));
            if (progress >= target) {
                completedThisRound = true;
                completedEver = true;
                return true;
            }
            return false;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public QuestEvent getEvent() { return event; }
        public int getTarget() { return target; }
        public int getProgress() { return progress; }
        public boolean isCompletedEver() { return completedEver; }
        public boolean isCompletedThisRound() { return completedThisRound; }
    }

    private final List<Quest> quests = new ArrayList<>();
    private final Random random = new Random();
    private Quest activeQuest;

    public QuestManager() {
        quests.add(new Quest("First Blood", "Defeat 1 enemy during a floor.", QuestEvent.KILL, 1));
        quests.add(new Quest("Clean Sweep", "Defeat 2 enemies during a floor.", QuestEvent.KILL, 2));
        quests.add(new Quest("Monster Hunter", "Defeat 3 enemies during a floor.", QuestEvent.KILL, 3));
        quests.add(new Quest("Executioner", "Defeat 5 enemies during a floor.", QuestEvent.KILL, 5));

        quests.add(new Quest("Scavenger", "Pick up 1 item during a floor.", QuestEvent.PICKUP, 1));
        quests.add(new Quest("Collector", "Pick up 2 items during a floor.", QuestEvent.PICKUP, 2));
        quests.add(new Quest("Relic Hoarder", "Pick up 3 items during a floor.", QuestEvent.PICKUP, 3));

        quests.add(new Quest("Keep Moving", "Survive 8 turns during a floor.", QuestEvent.TURN, 8));
        quests.add(new Quest("Steady Nerves", "Survive 15 turns during a floor.", QuestEvent.TURN, 15));
        quests.add(new Quest("Long Haul", "Survive 25 turns during a floor.", QuestEvent.TURN, 25));
        quests.add(new Quest("Against the Dark", "Survive 40 turns during a floor.", QuestEvent.TURN, 40));

        quests.add(new Quest("Find the Way", "Reach the exit and clear a floor.", QuestEvent.FLOOR_CLEAR, 1));
        quests.add(new Quest("Floor Breaker", "Clear a floor to earn your descent.", QuestEvent.FLOOR_CLEAR, 1));
    }

    public void resetRun() {
        for (Quest quest : quests) {
            quest.completedEver = false;
            quest.resetForRound();
        }
        startQuestForFloor(1);
    }

    public void startQuestForFloor(int floorNumber) {
        List<Quest> pool = new ArrayList<>();
        for (Quest quest : quests) {
            quest.resetForRound();
            if (!quest.isCompletedEver()) {
                pool.add(quest);
            }
        }

        if (pool.isEmpty()) {
            pool.addAll(quests);
        }

        activeQuest = pool.get(random.nextInt(pool.size()));
        activeQuest.resetForRound();
    }

    public boolean recordTurn() {
        return advance(QuestEvent.TURN, 1);
    }

    public boolean recordKills(int kills) {
        if (kills <= 0) return false;
        return advance(QuestEvent.KILL, kills);
    }

    public boolean recordPickup() {
        return advance(QuestEvent.PICKUP, 1);
    }

    public boolean recordFloorCleared() {
        return advance(QuestEvent.FLOOR_CLEAR, 1);
    }

    private boolean advance(QuestEvent event, int amount) {
        return activeQuest != null && activeQuest.advance(event, amount);
    }

    public Quest getActiveQuest() {
        return activeQuest;
    }

    public List<Quest> getQuests() {
        return Collections.unmodifiableList(quests);
    }

    public List<Quest> getCompletedQuests() {
        List<Quest> completed = new ArrayList<>();
        for (Quest quest : quests) {
            if (quest.isCompletedEver()) completed.add(quest);
        }
        return completed;
    }

    public List<Quest> getUncompletedQuests() {
        List<Quest> uncompleted = new ArrayList<>();
        for (Quest quest : quests) {
            if (!quest.isCompletedEver()) uncompleted.add(quest);
        }
        return uncompleted;
    }
}
