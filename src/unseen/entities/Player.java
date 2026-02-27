package unseen.entities;

import java.awt.Image;

import unseen.items.Item;
import unseen.map.Map;
import unseen.ui.GamePanel;
import unseen.game.Smoke;

import java.util.ArrayList;
import java.util.List;

public class Player extends Entity {
        public enum Facing { LEFT, RIGHT }
        private Facing facing = Facing.RIGHT;
    private GamePanel panel;
    private List<Item> inventory = new ArrayList<>();
    private Image heroImage;

    public Player(int x, int y) {
        super(x, y);
        // Load hero image
        try {
                java.net.URL url = Thread.currentThread().getContextClassLoader().getResource("unseen/assets/hero.png");            if (url != null) {
                heroImage = javax.imageio.ImageIO.read(url);
                System.out.println("Loaded hero.png successfully.");
            } else {
                System.out.println("hero.png not found in unseen/assets folder.");
                heroImage = null;
            }
        } catch (Exception e) {
            System.out.println("Error loading hero.png: " + e.getMessage());
            heroImage = null;
        }
        facing = Facing.RIGHT;
    }

    public Image getHeroImage() {
        return heroImage;
    }

    public Facing getFacing() {
        return facing;
    }
    public void setFacing(Facing facing) {
        this.facing = facing;
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
