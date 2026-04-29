package unseen.entities;

import unseen.ai.Pathfinder;
import unseen.game.Smoke;
import unseen.map.Map;
import unseen.utils.AssetLoader;
import unseen.utils.Constants;

import java.util.List;

public class SentryEnemy extends Enemy {

    private boolean canMove = false;
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
        tickAlertVisual();

        if (canSeePlayer(map, player, smokes)) {
            setState(State.CHASE);
            this.lastKnownX = player.getX();
            this.lastKnownY = player.getY();
        } else if (state == State.CHASE) {
            setState(State.SEARCH);
        } else {
            setState(State.PATROL);
        }

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

                if (tryAttackPlayerAt(next.x, next.y, player)) {
                    return;
                }

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
        setState(State.SEARCH);
    }

    public void handleAlerts(List<Enemy> allEnemies, Player player) {
        if (state == State.CHASE) {
            alertNearbyEnemies(allEnemies, Integer.MAX_VALUE, lastKnownX, lastKnownY);
            setAlertVisual(3);
        }
    }

    public void setAlertVisual(int turns) {
        this.alertVisual = true;
        this.alertDisplayTurns = Math.max(1, turns);
    }

    public void tickAlertVisual() {
        if (alertDisplayTurns > 0) {
            alertDisplayTurns--;
            if (alertDisplayTurns <= 0) {
                alertVisual = false;
                alertDisplayTurns = 0;
            }
        }
    }

    public boolean isAlertVisualActive() {
        return alertVisual;
    }
}
