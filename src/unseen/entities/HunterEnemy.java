package unseen.entities;

import unseen.ai.Node;
import unseen.ai.Pathfinder;
import unseen.game.StickyTrap;
import unseen.map.Map;
import unseen.utils.AssetLoader;
import unseen.utils.Constants;

import java.util.List;

public class HunterEnemy extends Enemy {

    private List<StickyTrap> traps;
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
                chase(map, player, allEnemies);
                break;
            case SEARCH:
                search();
                break;
            default:
                break;
        }
    }

    private void chase(Map map, Player player, List<Enemy> allEnemies) {
        int tx = lastKnownX;
        int ty = lastKnownY;

        if (isFlanker) {
            java.awt.Point flank = getFlankingTarget(map, player);
            tx = flank.x;
            ty = flank.y;
        }

        List<Node> path = pathfinder.findPath(map, x, y, tx, ty);
        if (path != null && path.size() > 1) {
            Node next = path.get(1);

            if (tryAttackPlayerAt(next.x, next.y, player)) {
                return;
            }

            if (isTileOccupied(next.x, next.y, allEnemies)) {
                return;
            }

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
