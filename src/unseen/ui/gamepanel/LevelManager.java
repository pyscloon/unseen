package unseen.ui.gamepanel;

import unseen.ai.AStar;
import unseen.ai.LineOfSight;
import unseen.ai.PathValidator;
import unseen.entities.Enemy;
import unseen.entities.HunterEnemy;
import unseen.entities.PatrolEnemy;
import unseen.entities.Player;
import unseen.entities.SentryEnemy;
import unseen.game.ActiveFlare;
import unseen.game.Smoke;
import unseen.game.SmokeSpawner;
import unseen.items.Flare;
import unseen.items.NoiseMaker;
import unseen.items.SmokeBomb;
import unseen.map.ExitPlacer;
import unseen.map.Map;
import unseen.map.MapGenerator;
import unseen.map.Tile;
import unseen.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;
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
    private List<Smoke> smokes = new ArrayList<>();
    private List<FlashEffect> noiseFlashes = new ArrayList<>();
    private List<ActiveFlare> flares = new ArrayList<>();
    private int floorNumber = 1;

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Map getMap() { return map; }

    public Player getPlayer() { return player; }

    public List<Enemy> getEnemies() { return enemies; }

    public boolean[][] getVisible() { return visible; }

    public float[][] getLightLevel() { return lightLevel; }

    public List<Smoke> getSmokes() { return smokes; }

    public List<FlashEffect> getNoiseFlashes() { return noiseFlashes; }

    public List<ActiveFlare> getFlares() { return flares; }

    public int getFloorNumber() { return floorNumber; }

    // -------------------------------------------------------------------------
    // Level setup / progression
    // -------------------------------------------------------------------------

    /**
     * Generates a new map and places enemies randomly.
     * Does NOT touch the player or call updateVisibility — caller must do that.
     */
    private void buildFloor() {

        PathValidator validator = new PathValidator();

        // Keep generating until valid
        do {
            map = MapGenerator.generate(); // also updates Constants.START_X/Y
            ExitPlacer.placeExit(map);
        }
        while (!validator.isValid(map));

        visible = new boolean[Constants.GRID_HEIGHT][Constants.GRID_WIDTH];
        lightLevel = new float[Constants.GRID_HEIGHT][Constants.GRID_WIDTH];
        smokes.clear();
        flares.clear();
        enemies = new ArrayList<>();

        Random rand = new Random();
        AStar pathfinder = new AStar();
        int minDist = 6;
        List<int[]> placed = new ArrayList<>();

        // Scale enemy count with floor: floor 1 = 3, +1 per floor, cap at 8
        int extraEnemies = Math.min(floorNumber - 1, 5);

        List<String> typeList =
                new ArrayList<>(Arrays.asList("patrol", "hunter", "sentry"));

        for (int i = 0; i < extraEnemies; i++) {
            typeList.add(i % 2 == 0 ? "patrol" : "hunter");
        }

        for (String type : typeList) {

            int ex = Constants.START_X;
            int ey = Constants.START_Y;

            for (int attempt = 0; attempt < 500; attempt++) {

                int cx = 1 + rand.nextInt(Constants.GRID_WIDTH - 2);
                int cy = 1 + rand.nextInt(Constants.GRID_HEIGHT - 2);

                int dist =
                        Math.abs(cx - Constants.START_X)
                                + Math.abs(cy - Constants.START_Y);

                if (map.getTile(cx, cy) == Tile.FLOOR
                        && dist >= minDist) {

                    boolean overlap =
                            placed.stream().anyMatch(p -> p[0] == cx && p[1] == cy);

                    if (!overlap) {
                        ex = cx;
                        ey = cy;
                        placed.add(new int[]{cx, cy});
                        break;
                    }
                }
            }

            switch (type) {

                case "patrol":
                    enemies.add(new PatrolEnemy(ex, ey, pathfinder));
                    break;

                case "hunter":
                    enemies.add(new HunterEnemy(ex, ey, pathfinder));
                    break;

                case "sentry":
                    enemies.add(new SentryEnemy(ex, ey, pathfinder));
                    break;
            }
        }
    }

    /** Full game reset: floor 1, new map, fresh player with starting items. */
    public void setupGame() {
        floorNumber = 1;
        buildFloor();
        player = new Player(Constants.START_X, Constants.START_Y);
        player.addItem(new NoiseMaker());
        player.addItem(new SmokeBomb());
        player.addItem(new Flare());
        player.setSmokeSpawner(this);
        updateVisibility();
    }

    /**
     * Advance to the next floor: increment counter, regenerate map, reposition
     * player.
     */
    public void nextFloor() {
        floorNumber++;
        buildFloor();
        player.setPosition(Constants.START_X, Constants.START_Y);
        // Refill items for the new floor
        player.getInventory().clear();
        player.addItem(new NoiseMaker());
        player.addItem(new SmokeBomb());
        player.addItem(new Flare());
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

        // Player vision (range 6)
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {

                if (LineOfSight.hasLineOfSight(map, px, py, x, y, 6, smokes)) {
                    visible[y][x] = true;
                    double dist = Math.hypot(px - x, py - y);
                    float light = (float) Math.max(0, 1.0 - (dist / 6.0));
                    lightLevel[y][x] = Math.max(lightLevel[y][x], light);
                }
            }
        }

        // Torch illumination — use LOS from each torch so walls block torch light
        final int TORCH_RANGE = 3;
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {

                if (map.getTile(x, y) == Tile.TORCH) {

                    for (int ty2 = 0; ty2 < Constants.GRID_HEIGHT; ty2++) {
                        for (int tx2 = 0; tx2 < Constants.GRID_WIDTH; tx2++) {

                            if (LineOfSight.hasLineOfSight(map, x, y, tx2, ty2, TORCH_RANGE)) {
                                visible[ty2][tx2] = true;
                                double dist = Math.hypot(x - tx2, y - ty2);
                                float light = (float) Math.max(0, 1.0 - (dist / TORCH_RANGE));
                                lightLevel[ty2][tx2] = Math.max(lightLevel[ty2][tx2], light);
                            }
                        }
                    }
                }
            }
        }

        // Active flares
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

    // -------------------------------------------------------------------------
    // Noise flash
    // -------------------------------------------------------------------------

    /** Register a NoiseMaker ripple effect at the given tile for 4 turns. */
    public void addNoiseFlash(int x, int y) {
        noiseFlashes.add(new FlashEffect(x, y, 4));
    }
}
