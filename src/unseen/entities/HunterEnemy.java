package unseen.entities;

import java.util.List;
import unseen.ai.Node;
import unseen.ai.Pathfinder;
import unseen.map.Map;
import unseen.utils.AssetLoader;
import unseen.utils.Constants;

public class HunterEnemy extends Enemy {

    public HunterEnemy(int x, int y, Pathfinder pathfinder) {
        super(x, y, Constants.HUNTER_DETECTION_RANGE, pathfinder);
        this.type = EnemyType.HUNTER;
        AssetLoader assets = AssetLoader.get();
        upImage = assets.enemyUp;
        // Use enemyBase (enemy.png) when facing down so it faces the camera by default
        downImage = assets.enemyBase;
        leftImage = assets.enemyLeft;
        rightImage = assets.enemyRight;
        enemyImage = assets.enemyBase;
    }

    @Override
    public void takeTurn(Map map, Player player, List<unseen.game.Smoke> smokes, List<Enemy> allEnemies) {

        if (!isDistracted() && canSeePlayer(map, player, smokes)) {
            state = State.CHASE;
            lastKnownX = player.getX();
            lastKnownY = player.getY();
        }

        switch (state) {
            case CHASE:
                chase(map, allEnemies);
                break;
            case SEARCH:
                search();
                break;
            default:
                break;
        }
    }

    private void chase(Map map, List<Enemy> allEnemies) {
        List<Node> path = pathfinder.findPath(map, x, y, lastKnownX, lastKnownY);
        if (path != null && path.size() > 1) {
            Node next = path.get(1);
            if (isTileOccupied(next.x, next.y, allEnemies))
                return; // blocked by another enemy
            if (next.x > x)
                setDirection(Direction.RIGHT);
            else if (next.x < x)
                setDirection(Direction.LEFT);
            else if (next.y > y)
                setDirection(Direction.DOWN);
            else if (next.y < y)
                setDirection(Direction.UP);
            x = next.x;
            y = next.y;
        } else {
            // Reached last known position — search for a few turns before giving up
            state = State.SEARCH;
            searchTurns = Constants.SEARCH_TURNS;
        }
    }

    private void search() {
        if (searchTurns <= 0) {
            state = State.PATROL;
            return;
        }
        searchTurns--;
    }
}
