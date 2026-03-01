package unseen.items;

import unseen.entities.Enemy;
import unseen.entities.Player;
import unseen.map.Map;

import java.util.List;

public class Flare extends Item {

    @Override
    public void use(Player player, Map map, List<Enemy> enemies) {
        // Ignored; flares must be thrown via useAt by GamePanel
    }

    public void useAt(Player player, Map map, int targetX, int targetY) {
        if (player.getSmokeSpawner() != null) {
            player.getSmokeSpawner().spawnFlare(targetX, targetY);
        }
    }
}
