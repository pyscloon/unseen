package unseen.entities;

import unseen.map.Map;
import unseen.ai.Pathfinder;
import unseen.ai.Node;
import unseen.game.Smoke;
import java.util.List;

/**
 * The Stalker is an invincible, fast-moving enemy that spawns 
 * when the player stays too long on a floor in Horror Mode.
 */
public class StalkerEnemy extends Enemy {
    
    private int stalkerSpeed = 2; // Moves 2 tiles per turn!

    public StalkerEnemy(int x, int y, Pathfinder pathfinder) {
        // High detection range (not that it matters as it always chases)
        super(x, y, 99, pathfinder);
        this.state = State.CHASE;
        this.type = EnemyType.STALKER;
        
        // Load images (placeholder using sentry images or similar)
        unseen.utils.AssetLoader loader = unseen.utils.AssetLoader.get();
        this.enemyImage = loader.sentry;
        this.leftImage = loader.sentry;
        this.rightImage = loader.sentry;
    }

    @Override
    public void takeTurn(Map map, Player player, List<Smoke> smokes, List<Enemy> enemies) {
        // The Stalker always knows where you are and ignores LOS for chasing.
        // It moves 2 or 3 times per turn (erratic and terrifying)
        int moves = (Math.random() < 0.3) ? 3 : 2;
        
        for (int i = 0; i < moves; i++) {
            List<Node> path = pathfinder.findPath(map, x, y, player.getX(), player.getY());
            if (path != null && path.size() > 1) {
                Node next = path.get(1);
                
                // Update direction based on move
                if (next.x > x) direction = Direction.RIGHT;
                else if (next.x < x) direction = Direction.LEFT;
                else if (next.y > y) direction = Direction.DOWN;
                else if (next.y < y) direction = Direction.UP;
                
                setPosition(next.x, next.y);

                // JUMPSCARE MECHANIC: If caught, trigger sound and vanish
                if (x == player.getX() && y == player.getY()) {
                    unseen.utils.SoundManager.get().play("spooky_jump", 1.4f);
                    this.alive = false; // "Go away so I can't see him anymore"
                    return;
                }
            }
        }
    }

    @Override
    public void die() {
        // Still invincible to normal damage, but vanishes after jumpscare
    }
}
