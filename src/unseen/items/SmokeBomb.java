package unseen.items;

import unseen.entities.*;
import unseen.game.SmokeSpawner;
import unseen.map.Map;

import java.util.List;

public class SmokeBomb extends Item {

    @Override
    public void use(Player player, Map map, List<Enemy> enemies) {
        SmokeSpawner spawner = player.getSmokeSpawner();
        if (spawner != null) {
            spawner.spawnSmoke(player.getX(), player.getY());
        }
    }
}
