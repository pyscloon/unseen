package unseen.entities;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import unseen.ai.Node;
import unseen.ai.Pathfinder;
import unseen.map.Map;
import unseen.utils.AssetLoader;
import unseen.utils.Constants;

public class PatrolEnemy extends Enemy {

    private static final java.util.Random RNG = new java.util.Random();

    public PatrolEnemy(int x, int y, Pathfinder pathfinder) {
        super(x, y, Constants.PATROL_DETECTION_RANGE, pathfinder);
        this.type = EnemyType.PATROL;
        AssetLoader assets = AssetLoader.get();
        upImage    = assets.enemyUp;
        downImage  = assets.enemyDown;
        leftImage  = assets.enemyLeft;
        rightImage = assets.enemyRight;
        enemyImage = assets.enemyBase;
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
        // Shuffle directions so patrol movement is unpredictable
        Integer[] indices = {0, 1, 2, 3};
        Collections.shuffle(Arrays.asList(indices), RNG);
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int i : indices) {
            int nx = x + dirs[i][0];
            int ny = y + dirs[i][1];
            if (map.isPassable(nx, ny)) {
                if      (nx > x) setDirection(Direction.RIGHT);
                else if (nx < x) setDirection(Direction.LEFT);
                else if (ny > y) setDirection(Direction.DOWN);
                else if (ny < y) setDirection(Direction.UP);
                x = nx;
                y = ny;
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
            // Set direction based on intended movement
            if (next.x > x) setDirection(Direction.RIGHT);
            else if (next.x < x) setDirection(Direction.LEFT);
            else if (next.y > y) setDirection(Direction.DOWN);
            else if (next.y < y) setDirection(Direction.UP);
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
