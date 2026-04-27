package unseen.entities;

import java.util.List;
import unseen.ai.Node;
import unseen.ai.Pathfinder;
import unseen.game.StickyTrap;
import unseen.map.Map;
import unseen.utils.AssetLoader;
import unseen.utils.Constants;

public class HunterEnemy extends Enemy {

    /** Traps list shared with LevelManager; null when floor ≤ 5. */
    private List<StickyTrap> traps;
    /** Cooldown (in turns) before the hunter can drop another trap. */
    private int trapCooldown = 0;

    public HunterEnemy(int x, int y, Pathfinder pathfinder) {
        this(x, y, pathfinder, null);
    }

    public HunterEnemy(int x, int y, Pathfinder pathfinder, List<StickyTrap> traps) {
        super(x, y, Constants.HUNTER_DETECTION_RANGE, pathfinder);
        this.type = EnemyType.HUNTER;
        this.traps = traps;
        AssetLoader assets = AssetLoader.get();
        upImage = assets.enemyUp;
        // Use enemyBase (enemy.png) when facing down so it faces the camera by default
        downImage = assets.enemyBase;
        leftImage = assets.enemyLeft;
        rightImage = assets.enemyRight;
        enemyImage = assets.enemyBase;
    }

    @Override
    public void takeTurn(Map map, Player player, List<unseen.game.Smoke> smokes, List<Enemy> allEnemies) {

        if (!isDistracted() && canSeePlayer(map, player, smokes)) {
            setState(State.CHASE);
            lastKnownX = player.getX();
            lastKnownY = player.getY();
        }

        switch (state) {
            case CHASE:
                chase(map, allEnemies);
                break;
            case SEARCH:
                search();
                break;
            default:
                break;
        }
    }

    private void chase(Map map, List<Enemy> allEnemies) {
        List<Node> path = pathfinder.findPath(map, x, y, lastKnownX, lastKnownY);
        if (path != null && path.size() > 1) {
            Node next = path.get(1);
            if (isTileOccupied(next.x, next.y, allEnemies))
                return; // blocked by another enemy

            // Drop a sticky trap on the tile the hunter is leaving (floor > 5)
            if (traps != null) {
                if (trapCooldown > 0) {
                    trapCooldown--;
                } else {
                    boolean alreadyTrapped = traps.stream()
                            .anyMatch(t -> t.getX() == x && t.getY() == y);
                    if (!alreadyTrapped) {
                        traps.add(new StickyTrap(x, y));
                        trapCooldown = 3;
                    }
                }
            }

            if (next.x > x)
                setDirection(Direction.RIGHT);
            else if (next.x < x)
                setDirection(Direction.LEFT);
            else if (next.y > y)
                setDirection(Direction.DOWN);
            else if (next.y < y)
                setDirection(Direction.UP);
            x = next.x;
            y = next.y;
        } else {
            // Reached last known position — search for a few turns before giving up
            setState(State.SEARCH);
            searchTurns = Constants.SEARCH_TURNS;
        }
    }

    private void search() {
        if (searchTurns <= 0) {
            setState(State.PATROL);
            return;
        }
        searchTurns--;
    }
}
