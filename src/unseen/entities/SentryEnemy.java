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
        upImage    = assets.enemyUp;
        downImage  = assets.enemyDown;
        leftImage  = assets.enemyLeft;
        rightImage = assets.enemyRight;
        enemyImage = assets.enemyBase;
    }

    @Override
    public void takeTurn(Map map, Player player, List<Smoke> smokes) {
        // Sentry is stationary — it only raises an alert when it spots the player.
        // Actual movement of other enemies is triggered via handleAlerts() in TurnManager.
        if (canSeePlayer(map, player, smokes)) {
            this.state = State.CHASE; // marks as alerted for the '!' visual
        }
    }

    /**
     * Called by TurnManager each turn so the sentry can alert nearby enemies
     * whenever it has spotted the player (state == CHASE).
     */
    public void handleAlerts(List<Enemy> allEnemies, Player player) {
        if (state == State.CHASE) {
            alertNearbyEnemies(allEnemies, 6, player.getX(), player.getY());
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
