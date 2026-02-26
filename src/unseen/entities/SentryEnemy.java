package unseen.entities;

import java.util.List;

import unseen.ai.Pathfinder;
import unseen.map.Map;
import unseen.utils.Constants;
import unseen.game.Smoke;


public class SentryEnemy extends Enemy {

    public SentryEnemy(int x, int y, Pathfinder pathfinder) {
        super(x, y, Constants.SENTRY_DETECTION_RANGE, pathfinder);
    }

    @Override
    public void takeTurn(Map map, Player player, List<Smoke> smokes) {

        if (canSeePlayer(map, player, player.getActiveSmokes())) {
            state = State.CHASE;
        }

    }
}
