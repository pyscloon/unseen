package unseen.items;

import unseen.entities.*;
import unseen.map.Map;

import java.util.List;

public class NoiseMaker extends Item {

    @Override
    public void use(Player player, Map map, List<Enemy> enemies) {

        for (Enemy e : enemies) {

            e.alertTo(player.getX(), player.getY());
        }
    }
}
