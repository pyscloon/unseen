package unseen.entities;

import java.util.List;
import unseen.ai.Pathfinder;
import unseen.map.Map;
import unseen.utils.AssetLoader;
import unseen.utils.Constants;
import unseen.game.Smoke;

public class SentryEnemy extends Enemy {

    // Visual alert state (shows '!' for a small number of turns)
    private boolean alertVisual = false;
    private int alertDisplayTurns = 0;

    public SentryEnemy(int x, int y, Pathfinder pathfinder) {
        super(x, y, Constants.SENTRY_DETECTION_RANGE, pathfinder);
        this.type = EnemyType.SENTRY;
        AssetLoader assets = AssetLoader.get();
        enemyImage = assets.sentry;
    }

    @Override
    public void takeTurn(Map map, Player player, List<Smoke> smokes, List<Enemy> allEnemies) {
        // Tick alert visual at the start so it doesn't decrement the same turn it's set
        tickAlertVisual();
        // Sentry is stationary — it only raises an alert when it spots the player.
        if (canSeePlayer(map, player, smokes)) {
            this.state = State.CHASE;
            // Record the real player position so handleAlerts broadcasts the correct
            // target.
            this.lastKnownX = player.getX();
            this.lastKnownY = player.getY();
        }
    }

    @Override
    public void redirectToNoise(int x, int y) {
        super.redirectToNoise(x, y);
        // Sentries do not chase noise, they just get "distracted" and stop their alarm
        this.state = State.SEARCH;
    }

    /**
     * Called by TurnManager each turn so the sentry can alert nearby enemies
     * whenever it has spotted the player (state == CHASE).
     */
    public void handleAlerts(List<Enemy> allEnemies, Player player) {
        if (state == State.CHASE) {
            // Use lastKnownX/Y — set to the player position when the sentry spots them
            // directly, or to the decoy position when alertTo() was called by an item.
            alertNearbyEnemies(allEnemies, Integer.MAX_VALUE, lastKnownX, lastKnownY);
            setAlertVisual(3);
        }
    }

    /* ----------------- Alert visual helpers ----------------- */

    // Turn on the visual indicator for `turns` turns
    public void setAlertVisual(int turns) {
        this.alertVisual = true;
        this.alertDisplayTurns = Math.max(1, turns);
    }

    // Called each application turn to decrement visual timer
    public void tickAlertVisual() {
        if (alertDisplayTurns > 0) {
            alertDisplayTurns--;
            if (alertDisplayTurns <= 0) {
                alertVisual = false;
                alertDisplayTurns = 0;
            }
        }
    }

    // Query helper used by the renderer
    public boolean isAlertVisualActive() {
        return alertVisual;
    }
}
