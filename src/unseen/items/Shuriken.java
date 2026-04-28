package unseen.items;

import unseen.entities.Player;
import unseen.entities.Enemy;
import unseen.map.Map;
import unseen.map.Tile;
import unseen.utils.Constants;
import java.util.List;

public class Shuriken extends Item {

    public static final int RANGE = 5;
    private int[] lastKillPos = null;

    public int[] getLastKillPos() { return lastKillPos; }

    @Override
    public void use(Player player, Map map, List<Enemy> enemies) {
        // Default: fire in the direction the player is facing
        int dx = (player.getFacing() == Player.Facing.RIGHT) ? 1 : -1;
        fireInDirection(player.getX(), player.getY(), dx, 0, map, enemies);
    }

    /** Throw in an explicit direction (dx, dy) -- one of the 4 cardinal directions. */
    public void fireInDirection(int px, int py, int dx, int dy, Map map, List<Enemy> enemies) {
        lastKillPos = null;
        for (int i = 1; i <= RANGE; i++) {
            int tx = px + dx * i;
            int ty = py + dy * i;
            if (tx < 0 || tx >= Constants.GRID_WIDTH || ty < 0 || ty >= Constants.GRID_HEIGHT) break;
            if (map.getTile(tx, ty) == Tile.WALL) break;
            for (Enemy e : enemies) {
                if (e.isAlive() && e.getX() == tx && e.getY() == ty) {
                    e.die();
                    lastKillPos = new int[]{tx, ty};
                    return;
                }
            }
        }
    }
}