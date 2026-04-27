package unseen.entities;

import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import unseen.ai.Node;
import unseen.ai.Pathfinder;
import unseen.map.Map;
import unseen.utils.AssetLoader;
import unseen.utils.Constants;

public class PatrolEnemy extends Enemy {

    private static final java.util.Random RNG = new java.util.Random();

    // Pre-flipped (horizontal mirror) version of the patrol sprite for rightward movement
    private final java.awt.Image patrolFlipped;

    public PatrolEnemy(int x, int y, Pathfinder pathfinder) {
        super(x, y, Constants.PATROL_DETECTION_RANGE, pathfinder);
        this.type = EnemyType.PATROL;
        AssetLoader assets = AssetLoader.get();
        enemyImage   = assets.patrol;
        patrolFlipped = buildFlipped(assets.patrol);
    }

    /** Creates a horizontally flipped copy of the source image. */
    private static java.awt.Image buildFlipped(java.awt.Image src) {
        if (src == null) return null;
        int w = src.getWidth(null);
        int h = src.getHeight(null);
        if (w <= 0 || h <= 0) return src;
        BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-w, 0);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        // draw original into buf first so we have a BufferedImage to operate on
        java.awt.Graphics2D g = buf.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return op.filter(buf, null);
    }

    @Override
    public java.awt.Image getEnemyImage() {
        if (direction == Direction.RIGHT && patrolFlipped != null) {
            return patrolFlipped;
        }
        return enemyImage; // patrol.png faces left by default
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
                search(map);
                break;

            case PATROL:
            default:
                patrol(map, allEnemies);
                break;
        }
    }

    private void patrol(Map map, List<Enemy> allEnemies) {
        // Shuffle directions so patrol movement is unpredictable
        Integer[] indices = {0, 1, 2, 3};
        Collections.shuffle(Arrays.asList(indices), RNG);
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int i : indices) {
            int nx = x + dirs[i][0];
            int ny = y + dirs[i][1];
            if (map.isPassable(nx, ny) && !isTileOccupied(nx, ny, allEnemies)) {
                if      (nx > x) setDirection(Direction.RIGHT);
                else if (nx < x) setDirection(Direction.LEFT);
                else if (ny > y) setDirection(Direction.DOWN);
                else if (ny < y) setDirection(Direction.UP);
                x = nx;
                y = ny;
                break;
            }
        }
    }

    private void chase(Map map, List<Enemy> allEnemies) {

        List<Node> path =
                pathfinder.findPath(map, x, y,
                        lastKnownX, lastKnownY);

        if (path != null && path.size() > 1) {
            Node next = path.get(1);
            if (isTileOccupied(next.x, next.y, allEnemies)) return; // blocked by another enemy
            // Set direction based on intended movement
            if (next.x > x) setDirection(Direction.RIGHT);
            else if (next.x < x) setDirection(Direction.LEFT);
            else if (next.y > y) setDirection(Direction.DOWN);
            else if (next.y < y) setDirection(Direction.UP);
            x = next.x;
            y = next.y;
        } else {
            setState(State.SEARCH);
            searchTurns = Constants.SEARCH_TURNS;
        }
    }

    private void search(Map map) {

        if (searchTurns <= 0) {
            setState(State.PATROL);
            return;
        }

        searchTurns--;
    }
}
