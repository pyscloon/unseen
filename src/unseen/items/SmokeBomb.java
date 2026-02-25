package unseen.items;

import unseen.entities.*;
import unseen.map.Map;

import java.util.List;

public class SmokeBomb extends Item {

    @Override
    public void use(Player player, Map map, List<Enemy> enemies) {

        // Temporarily disable enemy detection
        for (Enemy e : enemies) {
            e.calmDown();
        }
    }
}
