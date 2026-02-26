package unseen.entities;

import java.util.List;
import unseen.ai.Node;
import unseen.ai.Pathfinder;
import unseen.map.Map;
import unseen.utils.Constants;

public class HunterEnemy extends Enemy {

    public HunterEnemy(int x, int y, Pathfinder pathfinder) {
        super(x, y, Constants.HUNTER_DETECTION_RANGE, pathfinder);
        this.type = EnemyType.HUNTER;
        try {
            java.net.URL upUrl = Thread.currentThread().getContextClassLoader().getResource("unseen/assets/up-enemy.png");
            if (upUrl != null) upImage = javax.imageio.ImageIO.read(upUrl);
            java.net.URL downUrl = Thread.currentThread().getContextClassLoader().getResource("unseen/assets/down-enemy.png");
            if (downUrl != null) downImage = javax.imageio.ImageIO.read(downUrl);
            java.net.URL leftUrl = Thread.currentThread().getContextClassLoader().getResource("unseen/assets/left-enemy.png");
            if (leftUrl != null) leftImage = javax.imageio.ImageIO.read(leftUrl);
            java.net.URL rightUrl = Thread.currentThread().getContextClassLoader().getResource("unseen/assets/right-enemy.png");
            if (rightUrl != null) rightImage = javax.imageio.ImageIO.read(rightUrl);
            java.net.URL baseUrl = Thread.currentThread().getContextClassLoader().getResource("unseen/assets/enemy.png");
            if (baseUrl != null) enemyImage = javax.imageio.ImageIO.read(baseUrl);
        } catch (Exception e) { enemyImage = null; }
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
                // Set direction based on movement
                int dx = next.x - x;
                int dy = next.y - y;
                if (dx == 1) setDirection(Direction.RIGHT);
                else if (dx == -1) setDirection(Direction.LEFT);
                else if (dy == 1) setDirection(Direction.DOWN);
                else if (dy == -1) setDirection(Direction.UP);
                x = next.x;
                y = next.y;
            }
        }
    }
}
