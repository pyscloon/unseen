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
        upImage    = assets.enemyUp;
        downImage  = assets.enemyDown;
        leftImage  = assets.enemyLeft;
        rightImage = assets.enemyRight;
        enemyImage = assets.enemyBase;
    }

    @Override
    public void takeTurn(Map map, Player player, List<unseen.game.Smoke> smokes) {

        // use the passed smokes list
        if (canSeePlayer(map, player, smokes)) {
            state = State.CHASE;
            lastKnownX = player.getX();
            lastKnownY = player.getY();
        }

        if (state == State.CHASE) {
            List<Node> path =
                    pathfinder.findPath(map, x, y,
                            lastKnownX, lastKnownY);

            if (path != null && path.size() > 1) {
                Node next = path.get(1);
                if      (next.x > x) setDirection(Direction.RIGHT);
                else if (next.x < x) setDirection(Direction.LEFT);
                else if (next.y > y) setDirection(Direction.DOWN);
                else if (next.y < y) setDirection(Direction.UP);
                x = next.x;
                y = next.y;
            } else {
                // Reached last known position — give up chase
                state = State.PATROL;
            }
        }
    }
}
