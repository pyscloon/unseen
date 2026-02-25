package unseen.ai;

import unseen.map.Map;
import java.util.List;

public interface Pathfinder {
    List<Node> findPath(Map map, int startX, int startY, int targetX, int targetY);
}
