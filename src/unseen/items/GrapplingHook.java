package unseen.items;

import unseen.entities.Enemy;
import unseen.entities.Player;
import unseen.map.Map;
import unseen.map.Tile;

import java.util.Comparator;
import java.util.List;

public class GrapplingHook extends Item {

    @Override
    public void use(Player player, Map map, List<Enemy> enemies) {
        // Grappling Hook needs an aimed wall target via GamePanel.
    }

    public boolean useAt(Player player, Map map, List<Enemy> enemies, int wallX, int wallY) {
        if (!isWallTarget(map, wallX, wallY)) {
            return false;
        }

        int[] landing = findLandingSpot(player, map, enemies, wallX, wallY);
        if (landing == null) {
            return false;
        }

        if (landing[0] < player.getX()) {
            player.setFacing(Player.Facing.LEFT);
        } else if (landing[0] > player.getX()) {
            player.setFacing(Player.Facing.RIGHT);
        }

        player.setPosition(landing[0], landing[1]);
        return true;
    }

    public boolean isWallTarget(Map map, int x, int y) {
        return x >= 0 && y >= 0
                && x < unseen.utils.Constants.GRID_WIDTH
                && y < unseen.utils.Constants.GRID_HEIGHT
                && map.getTile(x, y) == Tile.WALL;
    }

    private int[] findLandingSpot(Player player, Map map, List<Enemy> enemies, int wallX, int wallY) {
        int[][] dirs = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };

        return java.util.Arrays.stream(dirs)
                .map(dir -> new int[] { wallX + dir[0], wallY + dir[1] })
                .filter(pos -> isValidLanding(map, enemies, pos[0], pos[1]))
                .min(Comparator.comparingInt(pos ->
                        Math.abs(pos[0] - player.getX()) + Math.abs(pos[1] - player.getY())))
                .orElse(null);
    }

    private boolean isValidLanding(Map map, List<Enemy> enemies, int x, int y) {
        if (!map.isPassable(x, y)) {
            return false;
        }
        for (Enemy enemy : enemies) {
            if (enemy.isAlive() && enemy.getX() == x && enemy.getY() == y) {
                return false;
            }
        }
        return true;
    }
}
