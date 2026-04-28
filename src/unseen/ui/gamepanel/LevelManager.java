package unseen.ui.gamepanel;

import unseen.ai.AStar;
import unseen.ai.LineOfSight;
import unseen.ai.PathValidator;
import unseen.entities.*;
import unseen.game.ActiveFlare;
import unseen.game.Smoke;
import unseen.game.SmokeSpawner;
import unseen.items.Flare;
import unseen.items.NoiseMaker;
import unseen.items.Shuriken;
import unseen.items.SmokeBomb;
import unseen.map.ExitPlacer;
import unseen.map.Map;
import unseen.map.MapGenerator;
import unseen.map.Tile;
import unseen.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Owns all game-world state and handles level generation, floor progression,
 * visibility computation, and smoke/flare/noise-flash lifecycle.
 */
public class LevelManager implements SmokeSpawner {

    private Map map;
    private Player player;
    private List<Enemy> enemies;
    private boolean[][] visible;
    private float[][] lightLevel;
    private unseen.ui.GamePanel panel;
    private int floorNumber = 1;
    private List<unseen.entities.ShadowFigure> shadowFigures = new ArrayList<>();

    public List<unseen.entities.ShadowFigure> getShadowFigures() {
        return shadowFigures;
    }

    private int turnCount = 0;
    private int stalkerSpawnCount = 0;
    private int darkEventTurns = 0;
    private int lanternFlickerTurns = 0;
    private boolean floorPurified = false;

    // Flickering Campfire (Horror Mode)
    private int flickerCampfireX = -1;
    private int flickerCampfireY = -1;
    private boolean flickerCampfireOn = true;

    private List<String> availableLore = new ArrayList<>();
    private java.util.Map<String, String> currentFloorLore = new HashMap<>();

    private static final String[] LORE_NOTES = {
            "Entry 1: They told me the artifact was here. I didn't listen to the warnings about the 'Unseen'.",
            "Entry 7: The walls... they move when I'm not looking. The blood isn't mine, but it feels familiar.",
            "Scrawled Note: Don't trust the lights. The campfires are just beacons for the things in the walls.",
            "Torn Page: I found a cross. It hums when they are near. I must get to the lower floors.",
            "Last Entry: If you find this, turn back. There is no exit, only deeper layers of the same nightmare.",
            "Ilon's Log: Logic fails here. We tried to treat it like a debug session in Room 205, but you can't breakpoint a jumpscare.",
            "Crumpled Paper: Dominic's code didn't work. The 'Unseen' don't follow syntax rules. They are the ultimate unhandled exception.",
            "Blood-stained Script: Ahron said the dungeon is like a recursive function with no base case. We're looping into the abyss.",
            "Software Student Diary: We were just kids from Room 205. Now we're just data points in their 'Unseen' experiment.",
            "Final Warning: Dominic, Ahron, Ilon... they're all gone. I'm the only variable left in this corrupt environment.",
            "Scrap: The campus feels like a lifetime ago. This isn't a dungeon, it's a server crash in physical form.",
            "Note: Room 205 was our sanctuary. Now it's just a memory. If you find our project, delete it. It's what let them in.",
            "Software Engineering Lab: They wanted 'innovative solutions'. We gave them a door to the void instead."
    };

    public void checkNoteAt(int x, int y) {
        if (map.getDecal(x, y) == unseen.map.DecalType.NOTE_SCRAP) {
            String key = x + "," + y;
            if (!currentFloorLore.containsKey(key)) {
                if (!availableLore.isEmpty()) {
                    currentFloorLore.put(key, availableLore.remove(0));
                } else {
                    currentFloorLore.put(key,
                            "The ink has faded into nothingness... the secrets are lost to the void.");
                }
            }
            panel.setCurrentNoteLore(currentFloorLore.get(key));
        } else {
            panel.setCurrentNoteLore(null);
        }
    }

    // --- SUSPENSE SYSTEM ---
    private int terrorLevel = 0; // 0 to 10
    private boolean highTensionMode = false;
    private int tensionTimer = 20;

    public int getDarkEventTurns() {
        return darkEventTurns;
    }

    public boolean isHighTension() {
        return highTensionMode;
    }

    public int getTerrorLevel() {
        return terrorLevel;
    }

    public void setPanel(unseen.ui.GamePanel p) {
        this.panel = p;
    }

    // --- MIRROR PHANTOM ---
    private int phantomX = -1, phantomY = -1;
    private int phantomTurns = 0;

    public int getPhantomX() {
        return phantomX;
    }

    public int getPhantomY() {
        return phantomY;
    }

    private List<Smoke> smokes = new ArrayList<>();
    private List<FlashEffect> noiseFlashes = new ArrayList<>();
    private List<ActiveFlare> flares = new ArrayList<>();
    private List<unseen.game.StickyTrap> traps = new ArrayList<>();
    private List<ShurikenProjectile> shurikenProjectiles = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Map getMap() {
        return map;
    }

    public Player getPlayer() {
        return player;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public boolean[][] getVisible() {
        return visible;
    }

    public float[][] getLightLevel() {
        return lightLevel;
    }

    public List<Smoke> getSmokes() {
        return smokes;
    }

    public List<FlashEffect> getNoiseFlashes() {
        return noiseFlashes;
    }

    public List<ActiveFlare> getFlares() {
        return flares;
    }

    public List<unseen.game.StickyTrap> getTraps() {
        return traps;
    }

    public List<ShurikenProjectile> getShurikenProjectiles() {
        return shurikenProjectiles;
    }

    /**
     * Spawns a flying shuriken visual from {@code originX,originY} in direction
     * {@code dx,dy}, traveling {@code travelTiles} tiles.
     */
    public void spawnShurikenFlight(int originX, int originY, int dx, int dy, int travelTiles) {
        shurikenProjectiles.add(new ShurikenProjectile(originX, originY, dx, dy, travelTiles));
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    // -------------------------------------------------------------------------
    // Level setup / progression
    // -------------------------------------------------------------------------

    public void incrementTurn() {
        turnCount++;
        if (panel.isHorrorMode() && !floorPurified) {
            // --- Update Suspense Cycle ---
            tensionTimer--;
            if (tensionTimer <= 0) {
                highTensionMode = !highTensionMode;
                tensionTimer = 15 + new Random().nextInt(15);
                if (highTensionMode) {
                    unseen.utils.SoundManager.get().play("heartbeat", 0.4f);
                }
            }

            // Calculate Terror Level based on nearest enemy
            double minD = Double.MAX_VALUE;
            for (Enemy e : enemies) {
                double d = Math.hypot(e.getX() - player.getX(), e.getY() - player.getY());
                if (d < minD)
                    minD = d;
            }
            if (minD < 5.0)
                terrorLevel = 10;
            else if (minD < 10.0)
                terrorLevel = 7;
            else
                terrorLevel = highTensionMode ? 5 : 2;

            // Chance to trigger total darkness - only in High Tension or when close to
            // enemy (REDUCED)
            if (darkEventTurns <= 0 && (highTensionMode || minD < 8.0) && Math.random() < 0.015) {
                darkEventTurns = 2 + (int) (Math.random() * 2); // 2 to 3 turns of darkness (shorter)
                unseen.utils.SoundManager.get().play("ghostwhisper", 1.0f); // Eerie whisper for lights out
                panel.triggerShake(10, 2f);
            } else if (darkEventTurns > 0) {
                darkEventTurns--;
            }

            // Random ambient horror sounds - frequency tied to terrorLevel
            double ambientRoll = Math.random();
            double chance = 0.001 + (terrorLevel * 0.002); // Slightly increased from ultra-rare
            if (ambientRoll < chance) {
                if (highTensionMode && Math.random() < 0.01) {
                    unseen.utils.SoundManager.get().play("jumpscare", 0.4f);
                } else {
                    float vol = 0.1f + (terrorLevel * 0.02f);
                    // Added 'breathing' back to the mix
                    unseen.utils.SoundManager.get().playRandom(vol, "laugh", "scream", "iseeyou", "breathing",
                            "suspense", "no_more");
                }
            }

            // --- UNRELIABLE LANTERN ---
            if (lanternFlickerTurns > 0) {
                lanternFlickerTurns--;
            } else if (panel.isHorrorMode() && Math.random() < 0.02) { // 2% chance per turn
                lanternFlickerTurns = 1 + new Random().nextInt(2);
            }

            // --- HEARTBEAT SOUND LOGIC ---
            boolean anyChase = false;
            double chaseDist = 999;
            for (Enemy e : enemies) {
                if (e.getState() == Enemy.State.CHASE || e instanceof unseen.entities.StalkerEnemy) {
                    anyChase = true;
                    double d = Math.hypot(e.getX() - player.getX(), e.getY() - player.getY());
                    if (d < chaseDist)
                        chaseDist = d;
                }
            }
            if (anyChase && chaseDist < 15) { // Increased distance range
                // Heartbeat frequency (Scales more aggressively now)
                double hbChance = 0.15 + (1.0 - (chaseDist / 15.0)) * 0.45;
                if (Math.random() < hbChance) {
                    unseen.utils.SoundManager.get().play("heartbeat", 1.0f); // Louder volume
                }
            }

            // --- LOW HEALTH HEARTBEAT ---
            if (player.getHealth() == 1 && Math.random() < 0.25) {
                unseen.utils.SoundManager.get().play("heartbeat", 0.8f);
            }

            // --- PLAYER BREATHING (General ambient tension) ---
            if (panel.isHorrorMode() && !floorPurified && panel.getGameState() == unseen.game.GameState.PLAYING
                    && Math.random() < 0.1) { // 4% chance per turn for ambient breathing
                unseen.utils.SoundManager.get().play("breathing", 0.3f);
                unseen.utils.SoundManager.get().play("heartbeat", 0.8f);
            }

            // Spawn Shadow Figures in the dark - extremely rare now
            double shadowChance = highTensionMode ? 0.04 : 0.01;
            if (Math.random() < shadowChance && shadowFigures.size() < 2) {
                Random r = new Random();
                for (int i = 0; i < 50; i++) {
                    int sx = r.nextInt(Constants.GRID_WIDTH);
                    int sy = r.nextInt(Constants.GRID_HEIGHT);
                    if (map.getTile(sx, sy) == Tile.FLOOR && !visible[sy][sx]) {
                        shadowFigures.add(new unseen.entities.ShadowFigure(sx, sy));
                        break;
                    }
                }
            }

            // Update Shadow Figures
            shadowFigures.removeIf(sf -> {
                boolean wasSeen = sf.isSeen();
                sf.update(player.getX(), player.getY(), visible, map);
                if (!wasSeen && sf.isSeen()) {
                    // Randomize what they hear for variety
                    unseen.utils.SoundManager.get().playRandom(0.2f, "laugh", "jumpscare");
                    unseen.utils.SoundManager.get().playRandom(0.3f, "jumpscare", "scream");
                    unseen.utils.SoundManager.get().playRandom(0.5f, "iseeyou", "scream");
                    panel.triggerShake(5, 1.5f); // Tiny shake
                }
                return sf.isSeen();
            });

            // Stalker spawning logic (up to 2 per floor)
            boolean isStalkerAlive = enemies.stream().anyMatch(e -> e instanceof unseen.entities.StalkerEnemy);
            if (!isStalkerAlive) {
                if (stalkerSpawnCount == 0 && turnCount > 40) {
                    stalkerSpawnCount++;
                    enemies.add(new unseen.entities.StalkerEnemy(Constants.START_X, Constants.START_Y,
                            new unseen.ai.AStar()));
                    unseen.utils.SoundManager.get().playRandom(1.2f, "no_more", "scream", "jumpscare");
                    panel.triggerShake(20, 3f);
                } else if (stalkerSpawnCount == 1 && turnCount > 80) {
                    stalkerSpawnCount++;
                    enemies.add(new unseen.entities.StalkerEnemy(Constants.START_X, Constants.START_Y,
                            new unseen.ai.AStar()));
                    unseen.utils.SoundManager.get().playRandom(1.0f, "scream", "ghostwhisper", "iseeyou"); // Different
                                                                                                          // sound for
                                                                                                          // 2nd
                                                                                                          // appearance
                    panel.triggerShake(30, 4f);
                }
            }

            // --- STALKER PROXIMITY SOUNDS ---
            if (isStalkerAlive) {
                for (unseen.entities.Enemy e : enemies) {
                    if (e instanceof unseen.entities.StalkerEnemy) {
                        double dist = Math.hypot(e.getX() - player.getX(), e.getY() - player.getY());

                        if (dist < 8) {
                            // Breathing (Increased frequency when stalker is stalking)
                            double breathChance = 0.05 + (1.0 - (dist / 8.0)) * 0.15;
                            if (Math.random() < breathChance) {
                                float breathVol = (float) (0.3 + (8.0 - dist) * 0.1);
                                unseen.utils.SoundManager.get().play("breathing", breathVol);
                                // Alongside/After breathing: Play a loud, sharp heartbeat
                                unseen.utils.SoundManager.get().play("heartbeat", 0.75f);
                            }

                            // "I see you" (Reduced frequency to 2%)
                            if (dist < 5 && Math.random() < 0.02) {
                                if (Math.random() < 0.6) {
                                    unseen.utils.SoundManager.get().play("iseeyou", 0.5f);
                                } else {
                                    unseen.utils.SoundManager.get().play("suspense", 0.4f);
                                }
                            }
                        }
                        break;
                    }
                }
            }

            // --- MIRROR PHANTOM (Interactive psychological scare) ---
            if (phantomTurns > 0) {
                phantomTurns--;
                if (phantomTurns == 0) {
                    phantomX = -1;
                    phantomY = -1;
                } else {
                    // If player moved (we're in turn increment), check if they moved TOWARDS
                    // phantom
                    double d = Math.hypot(phantomX - player.getX(), phantomY - player.getY());
                    if (d < 3.5) { // Player got quite close
                        // Move phantom away in the same direction
                        int dx = Integer.compare(phantomX, player.getX());
                        int dy = Integer.compare(phantomY, player.getY());
                        int nx = phantomX + dx;
                        int ny = phantomY + dy;
                        if (nx >= 0 && nx < Constants.GRID_WIDTH && ny >= 0 && ny < Constants.GRID_HEIGHT
                                && map.isPassable(nx, ny)) {
                            phantomX = nx;
                            phantomY = ny;
                        }
                    }
                }
            } else if (panel.isHorrorMode()) {
                // Higher chance in horror mode
                double spawnChance = 0.05;
                if (Math.random() < spawnChance) {
                    Random r = new Random();
                    for (int i = 0; i < 100; i++) { // More attempts to find a valid spot
                        int tx = r.nextInt(Constants.GRID_WIDTH);
                        int ty = r.nextInt(Constants.GRID_HEIGHT);
                        double d = Math.hypot(tx - player.getX(), ty - player.getY());
                        // Spawn IN VISION but at a distance
                        if (map.getTile(tx, ty) == Tile.FLOOR && visible[ty][tx] && d > 4 && d < 7) {
                            phantomX = tx;
                            phantomY = ty;
                            phantomTurns = 2; // Lasts 2 turns
                            break;
                        }
                    }
                }
            }
            // --- ECHOING FOOTSTEPS --- (Suspense element)
            if (highTensionMode && Math.random() < 0.15) {
                // Play a delayed footstep to make player paranoid
                new Thread(() -> {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ex) {
                    }
                    unseen.utils.SoundManager.get().playRandom(0.3f, "footstep1", "footstep2");
                }).start();
            }

            // --- WHISPERING WALLS ---
            if (highTensionMode && Math.random() < 0.12) {
                int px = player.getX();
                int py = player.getY();
                // Check neighbors for walls
                boolean nearWall = false;
                if (px > 0 && map.getTile(px - 1, py) == Tile.WALL)
                    nearWall = true;
                else if (px < Constants.GRID_WIDTH - 1 && map.getTile(px + 1, py) == Tile.WALL)
                    nearWall = true;
                else if (py > 0 && map.getTile(px, py - 1) == Tile.WALL)
                    nearWall = true;
                else if (py < Constants.GRID_HEIGHT - 1 && map.getTile(px, py + 1) == Tile.WALL)
                    nearWall = true;

                if (nearWall) {
                    unseen.utils.SoundManager.get().play("smoke", 0.45f); // Use smoke as a whisper
                }
            }
        }
    }

    /**
     * Generates a new map and places enemies randomly.
     * Does NOT touch the player or call updateVisibility -- caller must do that.
     */
    private void buildFloor() {

        PathValidator validator = new PathValidator();

        // Keep generating until valid
        do {
            map = MapGenerator.generate(panel.isHorrorMode()); // also updates Constants.START_X/Y
            ExitPlacer.placeExit(map);
        } while (!validator.isValid(map));

        visible = new boolean[Constants.GRID_HEIGHT][Constants.GRID_WIDTH];
        lightLevel = new float[Constants.GRID_HEIGHT][Constants.GRID_WIDTH];
        smokes.clear();
        flares.clear();
        traps.clear();
        shurikenProjectiles.clear();
        enemies = new ArrayList<>();
        shadowFigures.clear(); // Clear old figures

        Random rand = new Random();
        AStar pathfinder = new AStar();
        int minDist = 8;
        List<int[]> placed = new ArrayList<>();

        int count = 3 + (floorNumber / 2);
        if (panel.isHorrorMode()) {
            // Lessen enemies in early horror floors as requested
            if (floorNumber <= 2)
                count = 2;
            else if (floorNumber <= 5)
                count = 3;
        }

        for (int i = 0; i < count; i++) {

            int ex = 0, ey = 0;
            for (int attempt = 0; attempt < 500; attempt++) {
                int cx = 1 + rand.nextInt(Constants.GRID_WIDTH - 2);
                int cy = 1 + rand.nextInt(Constants.GRID_HEIGHT - 2);

                int dist = Math.abs(cx - Constants.START_X)
                        + Math.abs(cy - Constants.START_Y);

                if (map.getTile(cx, cy) == Tile.FLOOR
                        && dist >= minDist) {

                    boolean overlap = placed.stream().anyMatch(p -> p[0] == cx && p[1] == cy);

                    if (!overlap) {
                        ex = cx;
                        ey = cy;
                        placed.add(new int[] { cx, cy });
                        break;
                    }
                }
            }

            String type;
            double roll = rand.nextDouble();
            if (roll < 0.5)
                type = "patrol";
            else if (roll < 0.8)
                type = "sentry";
            else
                type = "hunter";

            switch (type) {

                case "patrol":
                    enemies.add(new PatrolEnemy(ex, ey, pathfinder));
                    break;

                case "hunter":
                    enemies.add(new HunterEnemy(ex, ey, pathfinder,
                            floorNumber > 5 ? traps : null));
                    break;

                case "sentry":
                    enemies.add(new SentryEnemy(ex, ey, pathfinder, panel.isHorrorMode()));
                    break;
            }
        }

        if (floorNumber % 3 == 0) {
            for (int attempt = 0; attempt < 500; attempt++) {
                int hx = 1 + rand.nextInt(Constants.GRID_WIDTH - 2);
                int hy = 1 + rand.nextInt(Constants.GRID_HEIGHT - 2);
                if (map.getTile(hx, hy) == Tile.FLOOR && (hx != Constants.START_X || hy != Constants.START_Y)) {
                    map.setItem(hx, hy, new unseen.items.Heart());
                    break;
                }
            }
        }

        // Pick a flickering campfire in Horror Mode
        flickerCampfireX = -1;
        flickerCampfireY = -1;
        if (panel.isHorrorMode()) {
            List<int[]> campfires = new ArrayList<>();
            for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
                for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                    if (map.getTile(x, y) == unseen.map.Tile.CAMPFIRE) {
                        campfires.add(new int[] { x, y });
                    }
                }
            }
            if (!campfires.isEmpty()) {
                Random frand = new Random();
                int[] pick = campfires.get(frand.nextInt(campfires.size()));
                flickerCampfireX = pick[0];
                flickerCampfireY = pick[1];
            }
        }
    }

    /** Full game reset: floor 1, new map, fresh player with starting items. */
    public void setupGame() {
        this.floorNumber = 1;
        this.turnCount = 0;
        this.stalkerSpawnCount = 0;
        this.darkEventTurns = 0;
        this.lanternFlickerTurns = 0;
        this.terrorLevel = 0;
        this.highTensionMode = false;
        this.tensionTimer = 20;
        this.floorPurified = false;

        this.availableLore = new ArrayList<>(Arrays.asList(LORE_NOTES));
        Collections.shuffle(this.availableLore);
        this.currentFloorLore.clear();

        this.player = new Player(Constants.START_X, Constants.START_Y);
        buildFloor();
        player.addItem(new NoiseMaker());
        player.addItem(new SmokeBomb());
        player.addItem(new Flare());
        player.addItem(new Shuriken());
        if (panel.isHorrorMode()) {
            player.addItem(new unseen.items.Cross());
        }
        player.setSmokeSpawner(this);
        updateVisibility();
    }

    /**
     * Advance to the next floor: increment counter, regenerate map, reposition
     * player.
     */
    public void nextFloor() {
        floorNumber++;
        turnCount = 0;
        stalkerSpawnCount = 0;
        darkEventTurns = 0;
        lanternFlickerTurns = 0;
        terrorLevel = 0;
        highTensionMode = false;
        tensionTimer = 20;
        floorPurified = false;
        this.currentFloorLore.clear();

        buildFloor();
        player.setPosition(Constants.START_X, Constants.START_Y);
        // We no longer clear inventory or refill items here,
        // allowing the player to bring their current items to the next floor.
        updateVisibility();
    }

    // -------------------------------------------------------------------------
    // Visibility
    // -------------------------------------------------------------------------

    public void updateVisibility() {

        // Reset visibility
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                visible[y][x] = false;
                lightLevel[y][x] = 0.0f;
            }
        }

        int px = player.getX();
        int py = player.getY();

        // Player vision
        int baseRange = panel.isHorrorMode() ? 3 : 6;

        // --- UNRELIABLE LANTERN (Turn-based flicker) ---
        if (panel.isHorrorMode()) {
            if (darkEventTurns > 0) {
                baseRange = 1;
            } else if (lanternFlickerTurns > 0) {
                baseRange = 2; // Near-total darkness
            }
        }

        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {

                if (LineOfSight.hasLineOfSight(map, px, py, x, y, baseRange, smokes)) {
                    visible[y][x] = true;
                    double dist = Math.hypot(px - x, py - y);
                    float light = (float) Math.max(0, 1.0 - Math.pow(dist / (double) baseRange, 1.3));
                    lightLevel[y][x] = Math.max(lightLevel[y][x], light);
                }
            }
        }

        // Torch illumination -- skip if in total darkness
        if (darkEventTurns <= 0) {
            final int TORCH_RANGE = 3;
            for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
                for (int x = 0; x < Constants.GRID_WIDTH; x++) {

                    if (map.getTile(x, y) == Tile.TORCH || map.getTile(x, y) == Tile.CAMPFIRE) {

                        double currentRange = TORCH_RANGE;

                        // --- CINEMATIC FLICKERING (Horror Mode) ---
                        if (panel.isHorrorMode() && x == flickerCampfireX && y == flickerCampfireY) {
                            long t = System.currentTimeMillis();

                            // Macro state: Long periods of light vs. instability
                            double macro = Math.sin(t * 0.0007); // ~9 second cycle

                            boolean isOff = false;
                            if (macro > 0.5) {
                                isOff = false;
                            } else if (macro < -0.85) {
                                isOff = true;
                            } else {
                                double jitter = Math.sin(t * 0.08) + Math.sin(t * 0.037) + Math.sin(t * 0.011);
                                isOff = jitter > 0.4;
                            }

                            if (isOff)
                                continue;

                            // Wobble the range slightly for a "living" fire effect
                            currentRange += Math.sin(t * 0.05) * 0.4;
                        }

                        for (int ty2 = 0; ty2 < Constants.GRID_HEIGHT; ty2++) {
                            for (int tx2 = 0; tx2 < Constants.GRID_WIDTH; tx2++) {

                                if (LineOfSight.hasLineOfSight(map, x, y, tx2, ty2, (int) Math.ceil(currentRange))) {
                                    visible[ty2][tx2] = true;
                                    double dist = Math.hypot(x - tx2, y - ty2);

                                    // Use a steeper falloff (squared) to create a circular look
                                    float light = (float) Math.max(0, 1.0 - Math.pow(dist / currentRange, 1.3));
                                    lightLevel[ty2][tx2] = Math.max(lightLevel[ty2][tx2], light);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active flares -- skip if in total darkness
        if (darkEventTurns <= 0) {
            for (ActiveFlare flare : flares) {
                int cx = flare.getX();
                int cy = flare.getY();
                int fr = flare.getRadius();
                for (int ty2 = 0; ty2 < Constants.GRID_HEIGHT; ty2++) {
                    for (int tx2 = 0; tx2 < Constants.GRID_WIDTH; tx2++) {
                        if (LineOfSight.hasLineOfSight(map, cx, cy, tx2, ty2, fr)) {
                            visible[ty2][tx2] = true;
                            double dist = Math.hypot(cx - tx2, cy - ty2);
                            float light = (float) Math.max(0, 1.0 - (dist / fr));
                            lightLevel[ty2][tx2] = Math.max(lightLevel[ty2][tx2], light);
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Per-turn updates
    // -------------------------------------------------------------------------

    public void updateSmoke() {

        List<Smoke> expired = new ArrayList<>();

        for (Smoke smoke : smokes) {
            smoke.decrease();
            if (smoke.isExpired()) {
                expired.add(smoke);
            }
        }

        smokes.removeAll(expired);

        // Decrement noise-flash countdowns, remove expired flashes
        List<FlashEffect> expiredFlashes = new ArrayList<>();
        for (FlashEffect f : noiseFlashes) {
            f.decrement();
            if (f.isExpired())
                expiredFlashes.add(f);
        }
        noiseFlashes.removeAll(expiredFlashes);

        // Update flares
        List<ActiveFlare> expiredFlares = new ArrayList<>();
        for (ActiveFlare flare : flares) {
            flare.decrease();
            if (flare.isExpired())
                expiredFlares.add(flare);
        }
        flares.removeAll(expiredFlares);

        // Always refresh FOV after a turn so rendering reflects the new state
        updateVisibility();
    }

    // -------------------------------------------------------------------------
    // SmokeSpawner implementation
    // -------------------------------------------------------------------------

    @Override
    public void spawnSmoke(int x, int y) {
        smokes.add(new Smoke(x, y, 2, 5)); // radius 2, lasts 5 turns
    }

    @Override
    public void spawnFlare(int x, int y) {
        flares.add(new ActiveFlare(x, y, 5, 10)); // radius 5, lasts 10 turns
    }

    public void spawnDeathPuff(int x, int y) {
        smokes.add(new Smoke(x, y, 1, 2)); // radius 1, lasts 2 turns
    }

    // -------------------------------------------------------------------------
    // Noise flash
    // -------------------------------------------------------------------------

    /** Register a NoiseMaker ripple effect at the given tile for 4 turns. */
    @Override
    public void addNoiseFlash(int x, int y) {
        noiseFlashes.add(new FlashEffect(x, y, 4));
    }

    @Override
    public void addHolyFlash(int x, int y) {
        noiseFlashes.add(new FlashEffect(x, y, 8, true)); // Longer duration (8 turns)
    }

    @Override
    public void purifyFloor() {
        this.floorPurified = true;
        this.darkEventTurns = 0;
        this.terrorLevel = 0;
        this.highTensionMode = false;
        this.phantomTurns = 0;
        this.phantomX = -1;
        this.shadowFigures.clear();
    }

    public boolean isFloorPurified() {
        return floorPurified;
    }
}
