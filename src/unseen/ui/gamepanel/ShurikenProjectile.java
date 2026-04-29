package unseen.ui.gamepanel;

import unseen.entities.Enemy;
import unseen.utils.Constants;

/**
 * A real-time animated projectile that flies from one tile to another.
 *
 * <p>The animation runs independently of the turn system using wall-clock
 * time, so it stays smooth at any frame rate.  Call {@link #isDone()} each
 * frame and remove it from the list when true.
 */
public class ShurikenProjectile {

    /** Pixel travel speed (tiles per second). */
    private static final float SPEED_TILES_PER_SEC = 10f;

    /** Full rotations per second while in flight. */
    private static final float ROTATIONS_PER_SEC = 8f;

    // Origin in tile coordinates
    private final int originX;
    private final int originY;

    // Direction vector (cardinal or diagonal)
    private final int dx;
    private final int dy;

    // How many tiles the shuriken travels before stopping
    private final int travelTiles;
    private final Enemy targetEnemy;
    private final Enemy.EnemyType targetEnemyType;
    private final int targetX;
    private final int targetY;
    private final boolean hitWall;

    // When the animation was spawned
    private final long startMs;

    // Total duration in milliseconds
    private final long durationMs;

    /**
     * @param originX     tile X the player threw from
     * @param originY     tile Y the player threw from
     * @param dx          direction X (-1 / 0 / +1)
     * @param dy          direction Y (-1 / 0 / +1)
     * @param travelTiles how many tiles until it stops (wall or edge)
     */
    public ShurikenProjectile(int originX, int originY, int dx, int dy, int travelTiles) {
        this(originX, originY, dx, dy, travelTiles, null, -1, -1, false);
    }

    public ShurikenProjectile(int originX, int originY, int dx, int dy, int travelTiles,
                              Enemy targetEnemy, int targetX, int targetY, boolean hitWall) {
        this.originX     = originX;
        this.originY     = originY;
        this.dx          = dx;
        this.dy          = dy;
        this.travelTiles = Math.max(1, travelTiles);
        this.targetEnemy = targetEnemy;
        this.targetEnemyType = targetEnemy != null ? targetEnemy.getType() : null;
        this.targetX = targetX;
        this.targetY = targetY;
        this.hitWall = hitWall;
        this.startMs     = System.currentTimeMillis();
        this.durationMs  = (long) (travelTiles / SPEED_TILES_PER_SEC * 1000f);
    }

    /** 0.0 = just spawned, 1.0 = reached destination. */
    public float getProgress() {
        long elapsed = System.currentTimeMillis() - startMs;
        return Math.min(1f, elapsed / (float) durationMs);
    }

    /** Current spin angle in degrees (for Graphics2D.rotate). */
    public double getAngleDeg() {
        long elapsed = System.currentTimeMillis() - startMs;
        return (elapsed / 1000.0) * ROTATIONS_PER_SEC * 360.0;
    }

    /**
     * Current pixel-precise center of the projectile.
     *
     * @return float[]{pixelX, pixelY}
     */
    public float[] getPixelPos() {
        int ts   = Constants.TILE_SIZE;
        float px = (originX + 0.5f) * ts;
        float py = (originY + 0.5f) * ts;
        float progress = getProgress();
        px += dx * travelTiles * ts * progress;
        py += dy * travelTiles * ts * progress;
        return new float[]{px, py};
    }

    public boolean isDone() { return getProgress() >= 1f; }

    // Exposed for the renderer
    public int getTravelTiles() { return travelTiles; }
    public int getOriginX()     { return originX; }
    public int getOriginY()     { return originY; }
    public int getDx()          { return dx; }
    public int getDy()          { return dy; }
    public Enemy getTargetEnemy() { return targetEnemy; }
    public Enemy.EnemyType getTargetEnemyType() { return targetEnemyType; }
    public int getTargetX() { return targetX; }
    public int getTargetY() { return targetY; }
    public boolean hitWall() { return hitWall; }
}
