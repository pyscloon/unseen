package unseen.entities;

import unseen.items.Item;
import unseen.map.Map;
import unseen.ui.GamePanel;
import unseen.game.Smoke;

import java.util.ArrayList;
import java.util.List;

public class Player extends Entity {
    private GamePanel panel;
    private List<Item> inventory = new ArrayList<>();

    public Player(int x, int y) {
        super(x, y);
    }

    public void addItem(Item item) {
        inventory.add(item);
    }

    public List<Item> getInventory() {
        return inventory;
    }

    public void useItem(int index, Map map, List<Enemy> enemies) {
        if (index >= 0 && index < inventory.size()) {
            Item item = inventory.get(index);
            item.use(this, map, enemies);
            inventory.remove(index); // consume item
        }
    }
    // Add getter/setter for panel
    public void setPanel(GamePanel panel) { this.panel = panel; }
    public GamePanel getPanel() { return panel; }

    // Add getter for smokes
    public List<Smoke> getActiveSmokes() {
        if (panel != null) return panel.getSmokes();
        return List.of(); // empty list if panel not set
    }
}
