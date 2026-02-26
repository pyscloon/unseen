package unseen.entities;

import java.util.List;
import unseen.ai.Pathfinder;
import unseen.map.Map;
import unseen.utils.Constants;

public class SentryEnemy extends Enemy {

    public SentryEnemy(int x, int y, Pathfinder pathfinder) {
        super(x, y, Constants.SENTRY_DETECTION_RANGE, pathfinder);
    }

    @Override
    public void takeTurn(Map map, Player player, List<unseen.game.Smoke> smokes) {

        if (canSeePlayer(map, player, smokes)) {
            state = State.CHASE;
        }

        // Sentry is stationary; CHASE behavior handled elsewhere if needed
    }
}
