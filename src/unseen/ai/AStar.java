package unseen.ai;

import unseen.map.Map;

import java.util.*;

public class AStar implements Pathfinder {

    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    @Override
    public List<Node> findPath(Map map, int startX, int startY,
                               int targetX, int targetY) {

        if (startX == targetX && startY == targetY) {
            return java.util.Collections.singletonList(new Node(startX, startY));
        }

        PriorityQueue<Node> openSet =
                new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));

        HashMap<Long, Node> allNodes = new HashMap<>();
        HashSet<Long> closedSet = new HashSet<>();

        Node start = new Node(startX, startY);
        start.gCost = 0;
        start.hCost = manhattan(startX, startY, targetX, targetY);
        start.fCost = start.hCost;

        openSet.add(start);
        allNodes.put(key(startX, startY), start);

        while (!openSet.isEmpty()) {

            Node current = openSet.poll();
            long currentKey = key(current.x, current.y);

            if (closedSet.contains(currentKey)) {
                continue;
            }

            if (current.x == targetX && current.y == targetY)
                return reconstructPath(current);

            closedSet.add(currentKey);

            for (int[] dir : DIRECTIONS) {

                int nx = current.x + dir[0];
                int ny = current.y + dir[1];
                long neighborKey = key(nx, ny);

                if (!map.isPassable(nx, ny)) continue;
                if (closedSet.contains(neighborKey)) continue;

                double tentativeG = current.gCost + 1;

                Node neighbor = allNodes.get(neighborKey);

                if (neighbor == null) {
                    neighbor = new Node(nx, ny);
                    neighbor.gCost = Double.POSITIVE_INFINITY;
                    allNodes.put(neighborKey, neighbor);
                }

                if (tentativeG < neighbor.gCost) {

                    neighbor.gCost = tentativeG;
                    neighbor.hCost = manhattan(nx, ny, targetX, targetY);
                    neighbor.fCost = neighbor.gCost + neighbor.hCost;
                    neighbor.parent = current;

                    openSet.add(neighbor);
                }
            }
        }

        return null;
    }

    private List<Node> reconstructPath(Node node) {
        java.util.LinkedList<Node> path = new java.util.LinkedList<>();
        while (node != null) {
            path.addFirst(node);
            node = node.parent;
        }
        return path;
    }

    private int manhattan(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    private long key(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }
}
