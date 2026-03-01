package unseen.entities;

import java.util.List;
import unseen.ai.Pathfinder;
import unseen.map.Map;
import unseen.ai.LineOfSight;
import unseen.game.Smoke;

public abstract class Enemy extends Entity {
    public enum Direction { UP, DOWN, LEFT, RIGHT }
    protected Direction direction = Direction.DOWN;
    protected java.awt.Image upImage;
    protected java.awt.Image downImage;
    protected java.awt.Image leftImage;
    protected java.awt.Image rightImage;
    protected java.awt.Image enemyImage;
    public enum EnemyType { PATROL, HUNTER, SENTRY }
    protected EnemyType type;

    public enum State { PATROL, CHASE, SEARCH }

    protected State state = State.PATROL;
    protected int detectionRange;
    protected Pathfinder pathfinder;

    protected int lastKnownX, lastKnownY;
    protected int searchTurns = 0;
    protected int distractedTurns = 0;

    public Enemy(int x, int y, int detectionRange, Pathfinder pathfinder) {
        super(x, y);
        this.direction = Direction.DOWN;
        this.detectionRange = detectionRange;
        this.pathfinder = pathfinder;
        // Image loaded in subclasses
    }

    public java.awt.Image getSlimeImage() {
        return enemyImage;
    }
    public java.awt.Image getEnemyImage() {
        // Return image based on direction.
        // LEFT/RIGHT are swapped here because the sprite sheets use the opposite naming convention.
        switch (direction) {
            case UP:
                return upImage != null ? upImage : enemyImage;
            case DOWN:
                return downImage != null ? downImage : enemyImage;
            case LEFT:
                return rightImage != null ? rightImage : enemyImage;
            case RIGHT:
                return leftImage != null ? leftImage : enemyImage;
            default:
                return enemyImage;
        }
    }

    public void setDirection(Direction dir) {
        this.direction = dir;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public EnemyType getType() {
        return type;
    }
    public abstract void takeTurn(Map map, Player player, List<Smoke> smokes, List<Enemy> allEnemies);

    /** Returns true if another enemy already occupies tile (tx, ty). */
    protected boolean isTileOccupied(int tx, int ty, List<Enemy> allEnemies) {
        if (allEnemies == null) return false;
        for (Enemy e : allEnemies) {
            if (e != this && e.getX() == tx && e.getY() == ty) return true;
        }
        return false;
    }

    protected boolean canSeePlayer(Map map, Player player, List<Smoke> smokes) {
        return LineOfSight.hasLineOfSight(
                map,
                x, y,
                player.getX(), player.getY(),
                detectionRange,
                smokes);
    }

    public void alertTo(int x, int y) {
        this.state = State.CHASE;
        this.lastKnownX = x;
        this.lastKnownY = y;
    }

    public void calmDown() {
        this.state = State.PATROL;
    }

    protected void alertNearbyEnemies(List<Enemy> allEnemies, int radius, int targetX, int targetY) {
        if (allEnemies == null) return;
        for (Enemy e : allEnemies) {
            if (e == this) continue;
            int d = manhattanDistance(this.x, this.y, e.x, e.y);
            if (d <= radius) {
                e.alertTo(targetX, targetY);
            }
        }
    }

    protected int manhattanDistance(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    public java.util.List<unseen.ai.Node> getPlannedPath(Map map, Player player) {
        if (pathfinder == null) return null;

        return pathfinder.findPath(
                map,
                this.x,
                this.y,
                player.getX(),
                player.getY()
        );
    }

    public State getState() {
        return this.state;
    }

    /** Legacy single-arg takeTurn kept for compatibility; delegates with null enemy list. */
    public final void takeTurn(Map map, Player player, List<Smoke> smokes) {
        takeTurn(map, player, smokes, null);
    }

    public boolean hasLineOfSightToPlayer(Map map, Player player, List<unseen.game.Smoke> smokes) {
        return canSeePlayer(map, player, smokes);
    }

    public java.awt.Point getPlannedMove(Map map, Player player) {
        if (pathfinder == null) return null;

        List<unseen.ai.Node> path = pathfinder.findPath(map, this.x, this.y, player.getX(), player.getY());
        if (path == null || path.size() < 2) return null;

        unseen.ai.Node next = path.get(1);
        return new java.awt.Point(next.x, next.y);
    }

    public void redirectToNoise(int x, int y) {
        this.lastKnownX = x;
        this.lastKnownY = y;
        this.state = State.CHASE;
        this.searchTurns = unseen.utils.Constants.SEARCH_TURNS;
        this.distractedTurns = 2; // blocks LOS re-detection for 2 turns
    }

    // Add this helper:
    protected boolean isDistracted() {
        if (distractedTurns > 0) {
            distractedTurns--;
            return true;
        }
        return false;
    }
}
