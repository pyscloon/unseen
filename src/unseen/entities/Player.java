package unseen.entities;

import java.awt.Image;

import unseen.items.Item;
import unseen.map.Map;
import unseen.game.SmokeSpawner;
import unseen.utils.AssetLoader;

import java.util.ArrayList;
import java.util.List;

public class Player extends Entity {

    public enum Facing { LEFT, RIGHT }
    private Facing facing = Facing.RIGHT;

    /** Narrow interface — Player no longer depends on GamePanel directly. */
    private SmokeSpawner smokeSpawner;

    private List<Item> inventory = new ArrayList<>();
    private final Image heroImage;

    public Player(int x, int y) {
        super(x, y);
        heroImage = AssetLoader.get().hero;
        facing = Facing.RIGHT;
    }

    public Image getHeroImage() { return heroImage; }

    public Facing getFacing() { return facing; }
    public void setFacing(Facing facing) { this.facing = facing; }

    public void addItem(Item item) { inventory.add(item); }
    public List<Item> getInventory() { return inventory; }

    public void useItem(int index, Map map, List<Enemy> enemies) {
        if (index >= 0 && index < inventory.size()) {
            Item item = inventory.get(index);
            item.use(this, map, enemies);
            inventory.remove(index);
        }
    }

    public void setSmokeSpawner(SmokeSpawner spawner) { this.smokeSpawner = spawner; }
    public SmokeSpawner getSmokeSpawner() { return smokeSpawner; }
}
