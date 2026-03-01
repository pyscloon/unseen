package unseen.ai;

import unseen.map.Map;
import unseen.game.Smoke;

import java.util.List;

public class LineOfSight {

    /**
     * Bresenham line-of-sight check.
     * Returns true when there is an unobstructed, non-smoked path from
     * (x1,y1) to (x2,y2) within maxRange (Manhattan distance).
     */
    public static boolean hasLineOfSight(
            Map map,
            int x1, int y1,
            int x2, int y2,
            int maxRange,
            List<Smoke> smokes) {

        int manhattan = Math.abs(x1 - x2) + Math.abs(y1 - y2);
        if (manhattan > maxRange) return false;
        if (x1 == x2 && y1 == y2) return true;

        int adx = Math.abs(x2 - x1);
        int ady = Math.abs(y2 - y1);
        int sx  = x1 < x2 ? 1 : -1;
        int sy  = y1 < y2 ? 1 : -1;
        int err = adx - ady;

        int cx = x1;
        int cy = y1;

        while (true) {
            int e2 = 2 * err;
            if (e2 > -ady) { err -= ady; cx += sx; }
            if (e2 <  adx) { err += adx; cy += sy; }

            // Wall blocks sight (skip destination — the target entity stands there)
            if (cx != x2 || cy != y2) {
                if (!map.isPassable(cx, cy)) return false;
            }

            // Smoke blocks sight — check every tile including the destination
            for (Smoke smoke : smokes) {
                int dxS = cx - smoke.getX();
                int dyS = cy - smoke.getY();
                if (dxS * dxS + dyS * dyS <= smoke.getRadius() * smoke.getRadius()) {
                    return false;
                }
            }

            if (cx == x2 && cy == y2) break;
        }

        // Also check whether the source tile (observer) itself is inside smoke
        for (Smoke smoke : smokes) {
            int dxS = x1 - smoke.getX();
            int dyS = y1 - smoke.getY();
            if (dxS * dxS + dyS * dyS <= smoke.getRadius() * smoke.getRadius()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Convenience overload — no smoke list required (used for torch illumination).
     */
    public static boolean hasLineOfSight(Map map, int x1, int y1, int x2, int y2, int maxRange) {
        return hasLineOfSight(map, x1, y1, x2, y2, maxRange, List.of());
    }
}
