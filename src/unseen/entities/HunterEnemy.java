package unseen.entities;

import java.util.List;
import unseen.ai.Node;
import unseen.ai.Pathfinder;
import unseen.map.Map;
import unseen.utils.Constants;

public class HunterEnemy extends Enemy {

    public HunterEnemy(int x, int y, Pathfinder pathfinder) {
        super(x, y, Constants.HUNTER_DETECTION_RANGE, pathfinder);
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
                x = next.x;
                y = next.y;
            }
        }
    }
}
