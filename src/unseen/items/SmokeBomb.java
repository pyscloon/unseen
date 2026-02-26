package unseen.items;

import unseen.entities.*;
import unseen.map.Map;
import unseen.ui.GamePanel;

import java.util.List;

public class SmokeBomb extends Item {

    @Override
    public void use(Player player, Map map, List<Enemy> enemies) {

        // Smoke effect handled by GamePanel
        GamePanel panel = player.getPanel(); // see below
        panel.spawnSmoke(player.getX(), player.getY());
    }
}
