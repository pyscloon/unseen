package unseen.entities;

import java.util.List;
import unseen.ai.Pathfinder;
import unseen.map.Map;
import unseen.utils.Constants;

public class SentryEnemy extends Enemy {

    public SentryEnemy(int x, int y, Pathfinder pathfinder) {
        super(x, y, Constants.SENTRY_DETECTION_RANGE, pathfinder);
        this.type = EnemyType.SENTRY;
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

        if (canSeePlayer(map, player, smokes)) {
            state = State.CHASE;
        }

        // Sentry is stationary; CHASE behavior handled elsewhere if needed
    }
}
