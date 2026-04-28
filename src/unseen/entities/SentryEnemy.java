package unseen.entities;

import java.util.List;
import unseen.ai.Pathfinder;
import unseen.map.Map;
import unseen.utils.AssetLoader;
import unseen.utils.Constants;
import unseen.game.Smoke;

public class SentryEnemy extends Enemy {

    private boolean canMove = false;
    // Visual alert state (shows '!' for a small number of turns)
    private boolean alertVisual = false;
    private int alertDisplayTurns = 0;

    public SentryEnemy(int x, int y, Pathfinder pathfinder, boolean canMove) {
        super(x, y, Constants.SENTRY_DETECTION_RANGE, pathfinder);
        this.type = EnemyType.SENTRY;
        this.canMove = canMove;
        AssetLoader assets = AssetLoader.get();
        enemyImage = assets.sentry;
    }

    @Override
    public void takeTurn(Map map, Player player, List<Smoke> smokes, List<Enemy> allEnemies) {
        // Tick alert visual at the start so it doesn't decrement the same turn it's set
        tickAlertVisual();
        
        // Sentry is usually stationary — it raises an alert when it spots the player.
        if (canSeePlayer(map, player, smokes)) {
            setState(State.CHASE);
            this.lastKnownX = player.getX();
            this.lastKnownY = player.getY();
        } else if (state == State.CHASE) {
            // Keep searching if we were just chasing
            setState(State.SEARCH);
        } else {
            setState(State.PATROL);
        }

        // CHANCE to actually move towards player if in CHASE mode (25% chance)
        // ONLY if movement is enabled (e.g. in Horror Mode)
        if (canMove && state == State.CHASE && Math.random() < 0.25) {
            int tx = lastKnownX;
            int ty = lastKnownY;
            if (isFlanker) {
                java.awt.Point flank = getFlankingTarget(map, player);
                tx = flank.x;
                ty = flank.y;
            }
            List<unseen.ai.Node> path = pathfinder.findPath(map, x, y, tx, ty);
            if (path != null && path.size() > 1) {
                unseen.ai.Node next = path.get(1);
                if (!isTileOccupied(next.x, next.y, allEnemies)) {
                    this.x = next.x;
                    this.y = next.y;
                }
            }
        }
    }

    @Override
    public void redirectToNoise(int x, int y) {
        super.redirectToNoise(x, y);
        // Sentries do not chase noise, they just get "distracted" and stop their alarm
        setState(State.SEARCH);
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
