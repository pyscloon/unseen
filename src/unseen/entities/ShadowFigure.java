package unseen.entities;

import unseen.map.Map;
import unseen.utils.Constants;

/**
 * A psychological horror element. Not a real enemy, but a figure that stands
 * in the darkness and vanishes when approached or seen.
 */
public class ShadowFigure {
    private int x, y;
    private boolean seen = false;

    public ShadowFigure(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isSeen() { return seen; }

    public void update(int px, int py, boolean[][] visible, Map map) {
        // If the player can see this tile, the figure vanishes
        if (visible[y][x]) {
            seen = true;
            return;
        }

        // If the player is very close, it also vanishes even if they haven't "seen" it yet
        double dist = Math.hypot(x - px, y - py);
        if (dist < 2.5) {
            seen = true;
        }
    }
}
