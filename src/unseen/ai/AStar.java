package unseen.ai;

import unseen.map.Map;
import unseen.utils.Constants;

import java.util.*;

public class AStar implements Pathfinder {

    @Override
    public List<Node> findPath(Map map, int startX, int startY,
                               int targetX, int targetY) {

        PriorityQueue<Node> openSet =
                new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));

        HashMap<String, Node> allNodes = new HashMap<>();
        HashSet<String> closedSet = new HashSet<>();

        Node start = new Node(startX, startY);
        start.gCost = 0;
        start.hCost = manhattan(startX, startY, targetX, targetY);
        start.fCost = start.hCost;

        openSet.add(start);
        allNodes.put(key(startX, startY), start);

        while (!openSet.isEmpty()) {

            Node current = openSet.poll();

            if (current.x == targetX && current.y == targetY)
                return reconstructPath(current);

            closedSet.add(key(current.x, current.y));

            for (int[] dir : directions()) {

                int nx = current.x + dir[0];
                int ny = current.y + dir[1];

                if (!map.isPassable(nx, ny)) continue;
                if (closedSet.contains(key(nx, ny))) continue;

                double tentativeG = current.gCost + 1;

                Node neighbor = allNodes.getOrDefault(
                        key(nx, ny), new Node(nx, ny));

                if (!allNodes.containsKey(key(nx, ny))
                        || tentativeG < neighbor.gCost) {

                    neighbor.gCost = tentativeG;
                    neighbor.hCost = manhattan(nx, ny, targetX, targetY);
                    neighbor.fCost = neighbor.gCost + neighbor.hCost;
                    neighbor.parent = current;

                    allNodes.put(key(nx, ny), neighbor);

                    openSet.remove(neighbor);
                    openSet.add(neighbor);
                }
            }
        }

        return null;
    }

    private List<Node> reconstructPath(Node node) {
        List<Node> path = new ArrayList<>();
        while (node != null) {
            path.add(0, node);
            node = node.parent;
        }
        return path;
    }

    private int manhattan(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    private int[][] directions() {
        return new int[][]{{1,0},{-1,0},{0,1},{0,-1}};
    }

    private String key(int x, int y) {
        return x + "," + y;
    }
}
