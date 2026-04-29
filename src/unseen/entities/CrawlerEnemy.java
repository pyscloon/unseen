package unseen.entities;

import unseen.ai.Node;
import unseen.ai.Pathfinder;
import unseen.game.Smoke;
import unseen.map.Map;
import unseen.utils.AssetLoader;
import unseen.utils.Constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The Crawler — a blind, fast enemy that hunts purely by proximity and sound.
 *
 * Behaviour:
 *  - WANDER (PATROL state): erratic random movement, 1 step per turn.
 *  - CHASE  (CHASE  state): 2 A* steps per turn toward last heard/felt position.
 *  - Trigger conditions (both flip it to CHASE):
 *      1. Player steps within PROXIMITY_RANGE tiles (Manhattan distance).
 *      2. redirectToNoise() is called (NoiseMaker, splash, etc.).
 *  - It NEVER uses line-of-sight — smoke bombs have zero effect on it.
 *  - After reaching its target with no new input it SEARCHES briefly then
 *    reverts to WANDER.
 *
 * Counterplay:
 *  - Shuriken one-shots it.
 *  - Noise Maker lures it away.
 *  - Stay >= (PROXIMITY_RANGE + 1) tiles away and move quietly.
 */
public class CrawlerEnemy extends Enemy {

    /** Manhattan radius within which the Crawler "feels" the player's footsteps. */
    private static final int PROXIMITY_RANGE = 3;

    /** Steps taken per turn while chasing. */
    private static final int CHASE_STEPS = 2;

    private static final java.util.Random RNG = new java.util.Random();

    public CrawlerEnemy(int x, int y, Pathfinder pathfinder) {
        super(x, y, PROXIMITY_RANGE, pathfinder);
        this.type = EnemyType.CRAWLER;

        AssetLoader assets = AssetLoader.get();
        enemyImage = assets.crawler != null ? assets.crawler : assets.patrol;

        upImage = enemyImage;
        downImage = enemyImage;
        leftImage = enemyImage;
        rightImage = enemyImage;
    }

    @Override
    public void takeTurn(Map map, Player player, List<Smoke> smokes, List<Enemy> allEnemies) {

        int dist = Math.abs(x - player.getX()) + Math.abs(y - player.getY());
        if (dist <= PROXIMITY_RANGE) {
            lastKnownX = player.getX();
            lastKnownY = player.getY();

            if (state != State.CHASE) {
                setState(State.CHASE);
                invalidatePathCache();
            }
        }

        switch (state) {
            case CHASE:
                chase(map, player, allEnemies);
                break;
            case SEARCH:
                search();
                break;
            case PATROL:
            default:
                wander(map, player, allEnemies);
                break;
        }
    }

    private void wander(Map map, Player player, List<Enemy> allEnemies) {
        Integer[] indices = {0, 1, 2, 3};
        Collections.shuffle(Arrays.asList(indices), RNG);

        int[][] dirs = {
                {1, 0},
                {-1, 0},
                {0, 1},
                {0, -1}
        };

        for (int i : indices) {
            int nx = x + dirs[i][0];
            int ny = y + dirs[i][1];

            if (map.isPassable(nx, ny) && !isTileOccupied(nx, ny, allEnemies)) {
                if (tryAttackPlayerAt(nx, ny, player)) {
                    return;
                }

                updateDirection(nx, ny);
                x = nx;
                y = ny;
                return;
            }
        }
    }

    private void chase(Map map, Player player, List<Enemy> allEnemies) {
        for (int step = 0; step < CHASE_STEPS; step++) {
            int dist = Math.abs(x - player.getX()) + Math.abs(y - player.getY());
            if (dist <= PROXIMITY_RANGE) {
                lastKnownX = player.getX();
                lastKnownY = player.getY();
            }

            List<Node> path = pathfinder.findPath(map, x, y, lastKnownX, lastKnownY);

            if (path == null || path.size() < 2) {
                setState(State.SEARCH);
                searchTurns = Constants.SEARCH_TURNS;
                return;
            }

            Node next = path.get(1);

            if (tryAttackPlayerAt(next.x, next.y, player)) {
                invalidatePathCache();
                return;
            }

            if (isTileOccupied(next.x, next.y, allEnemies)) {
                return;
            }

            updateDirection(next.x, next.y);
            x = next.x;
            y = next.y;

            invalidatePathCache();
        }
    }

    private void search() {
        if (searchTurns <= 0) {
            setState(State.PATROL);
            return;
        }

        searchTurns--;
    }

    @Override
    public void redirectToNoise(int nx, int ny) {
        this.lastKnownX = nx;
        this.lastKnownY = ny;
        setState(State.CHASE);
        this.searchTurns = Constants.SEARCH_TURNS + 2;
        invalidatePathCache();
    }

    private void updateDirection(int nx, int ny) {
        if (nx > x) {
            setDirection(Direction.RIGHT);
        } else if (nx < x) {
            setDirection(Direction.LEFT);
        } else if (ny > y) {
            setDirection(Direction.DOWN);
        } else if (ny < y) {
            setDirection(Direction.UP);
        }
    }

    public boolean isHunting() {
        return state == State.CHASE;
    }

    public static int getProximityRange() {
        return PROXIMITY_RANGE;
    }
}
