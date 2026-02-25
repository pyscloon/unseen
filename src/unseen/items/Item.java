package unseen.items;

import unseen.entities.Player;
import unseen.map.Map;
import unseen.entities.Enemy;

import java.util.List;

public abstract class Item {
    public abstract void use(Player player, Map map, List<Enemy> enemies);
}
