package unseen.entities;

import java.util.List;
import unseen.ai.Node;
import unseen.ai.Pathfinder;
import unseen.map.Map;
import unseen.utils.Constants;

public class PatrolEnemy extends Enemy {

    public PatrolEnemy(int x, int y, Pathfinder pathfinder) {
        super(x, y, Constants.PATROL_DETECTION_RANGE, pathfinder);
    }

    @Override
    public void takeTurn(Map map, Player player, List<unseen.game.Smoke> smokes) {

        if (canSeePlayer(map, player, smokes)) {
            state = State.CHASE;
            lastKnownX = player.getX();
            lastKnownY = player.getY();
        }

        switch (state) {

            case CHASE:
                chase(map, player);
                break;

            case SEARCH:
                search(map);
                break;

            case PATROL:
            default:
                patrol(map);
                break;
        }
    }

    private void patrol(Map map) {
        // simple random patrol
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (map.isPassable(nx, ny)) {
                x = nx; y = ny;
                break;
            }
        }
    }

    private void chase(Map map, Player player) {

        List<Node> path =
                pathfinder.findPath(map, x, y,
                        player.getX(), player.getY());

        if (path != null && path.size() > 1) {
            Node next = path.get(1);
            x = next.x;
            y = next.y;
        } else {
            state = State.SEARCH;
            searchTurns = Constants.SEARCH_TURNS;
        }
    }

    private void search(Map map) {

        if (searchTurns <= 0) {
            state = State.PATROL;
            return;
        }

        searchTurns--;
    }
}
