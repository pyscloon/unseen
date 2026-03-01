package unseen.items;

import unseen.entities.*;
import unseen.map.Map;
import unseen.map.Tile;
import unseen.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NoiseMaker extends Item {

    private static final Random RNG = new Random();
    /** Minimum Manhattan distance from the player for the decoy noise source. */
    private static final int MIN_DECOY_DIST = 5;

    /** Stores the last tile chosen as the decoy target so the UI can show a flash. */
    private int[] lastDecoyTarget = null;

    @Override
    public void use(Player player, Map map, List<Enemy> enemies) {

        int px = player.getX();
        int py = player.getY();

        // Collect floor tiles that are far enough from the player
        List<int[]> candidates = new ArrayList<>();
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                Tile t = map.getTile(x, y);
                if ((t == Tile.FLOOR || t == Tile.START) &&
                        Math.abs(x - px) + Math.abs(y - py) >= MIN_DECOY_DIST) {
                    candidates.add(new int[]{x, y});
                }
            }
        }

        if (candidates.isEmpty()) return;

        // Pick a random distant tile as the decoy noise source
        int[] target = candidates.get(RNG.nextInt(candidates.size()));
        lastDecoyTarget = target;
        for (Enemy e : enemies) {
            e.alertTo(target[0], target[1]);
        }
    }

    /**
     * Use the NoiseMaker at a specific player-chosen tile.
     * Enemies are alerted to that exact position.
     */
    public void useAt(Player player, Map map, List<Enemy> enemies, int targetX, int targetY) {
        lastDecoyTarget = new int[]{targetX, targetY};
        for (Enemy e : enemies) {
            e.alertTo(targetX, targetY);
        }
    }

    /** Returns the tile coordinates [x, y] of the last decoy placed, or null if unused. */
    public int[] getLastDecoyTarget() {
        return lastDecoyTarget;
    }
}
