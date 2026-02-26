package unseen.ai;

import unseen.map.Map;
import unseen.game.Smoke;

import java.util.List;

public class LineOfSight {

    public static boolean hasLineOfSight(
            Map map,
            int x1, int y1,
            int x2, int y2,
            int maxRange,
            List<Smoke> smokes) {

        int dx = Integer.compare(x2, x1);
        int dy = Integer.compare(y2, y1);

        int distance = Math.abs(x1 - x2) + Math.abs(y1 - y2);
        if (distance > maxRange) return false;

        int cx = x1;
        int cy = y1;

        while (cx != x2 || cy != y2) {

            cx += dx;
            cy += dy;

            // Wall blocks
            if (!map.isPassable(cx, cy))
                return false;

            // Smoke blocks
            for (Smoke smoke : smokes) {

                int dxSmoke = cx - smoke.getX();
                int dySmoke = cy - smoke.getY();

                if (dxSmoke * dxSmoke + dySmoke * dySmoke
                        <= smoke.getRadius() * smoke.getRadius()) {

                    return false;
                }
            }
        }

        return true;
    }
}
