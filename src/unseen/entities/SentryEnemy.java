package unseen.entities;

import unseen.ai.Pathfinder;
import unseen.map.Map;
import unseen.utils.Constants;

public class SentryEnemy extends Enemy {

    public SentryEnemy(int x, int y, Pathfinder pathfinder) {
        super(x, y, Constants.SENTRY_DETECTION_RANGE, pathfinder);
    }

    @Override
    public void takeTurn(Map map, Player player) {

        if (canSeePlayer(map, player)) {
            state = State.CHASE;
        }

    }
}
