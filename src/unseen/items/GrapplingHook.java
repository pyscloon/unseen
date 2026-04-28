package unseen.items;

import unseen.ai.LineOfSight;
import unseen.entities.Enemy;
import unseen.entities.Player;
import unseen.map.Map;
import unseen.map.Tile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GrapplingHook extends Item {

    @Override
    public void use(Player player, Map map, List<Enemy> enemies) {
        // Grappling Hook needs an aimed wall target via GamePanel.
    }

    public boolean useAt(Player player, Map map, List<Enemy> enemies, int wallX, int wallY) {
        if (!isValidWallTarget(player, map, wallX, wallY)) {
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

    public int[] findLandingSpot(Player player, Map map, List<Enemy> enemies, int wallX, int wallY) {
        if (!isValidWallTarget(player, map, wallX, wallY)) {
            return null;
        }
        return findLandingSpotInternal(player, map, enemies, wallX, wallY);
    }

    public int countEnemiesInPath(Player player, Map map, List<Enemy> enemies, int wallX, int wallY) {
        int[] landing = findLandingSpot(player, map, enemies, wallX, wallY);
        if (landing == null) {
            return 0;
        }

        int hits = 0;
        for (int[] tile : getLineTiles(player.getX(), player.getY(), landing[0], landing[1])) {
            for (Enemy enemy : enemies) {
                if (enemy.isAlive() && enemy.getX() == tile[0] && enemy.getY() == tile[1]) {
                    hits++;
                }
            }
        }
        return hits;
    }

    public boolean isWallTarget(Map map, int x, int y) {
        return x >= 0 && y >= 0
                && x < unseen.utils.Constants.GRID_WIDTH
                && y < unseen.utils.Constants.GRID_HEIGHT
                && map.getTile(x, y) == Tile.WALL;
    }

    public boolean isValidWallTarget(Player player, Map map, int x, int y) {
        if (!isWallTarget(map, x, y)) {
            return false;
        }

        boolean cardinal = player.getX() == x || player.getY() == y;
        if (!cardinal || (player.getX() == x && player.getY() == y)) {
            return false;
        }

        return LineOfSight.hasLineOfSight(
                map,
                player.getX(), player.getY(),
                x, y,
                unseen.utils.Constants.GRID_WIDTH + unseen.utils.Constants.GRID_HEIGHT);
    }

    private int[] findLandingSpotInternal(Player player, Map map, List<Enemy> enemies, int wallX, int wallY) {
        int[][] dirs = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };

        return java.util.Arrays.stream(dirs)
                .map(dir -> new int[] { wallX + dir[0], wallY + dir[1] })
                .filter(pos -> isValidLanding(map, enemies, pos[0], pos[1]))
                .min(Comparator.comparingInt(pos ->
                        Math.abs(pos[0] - player.getX()) + Math.abs(pos[1] - player.getY())))
                .orElse(null);
    }

    private List<int[]> getLineTiles(int x0, int y0, int x1, int y1) {
        List<int[]> tiles = new ArrayList<>();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;

        while (!(x == x1 && y == y1)) {
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
            tiles.add(new int[] { x, y });
        }

        return tiles;
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
