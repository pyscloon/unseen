    package unseen.entities;

    import unseen.game.SmokeSpawner;
    import unseen.items.Item;
    import unseen.map.Map;
    import unseen.utils.AssetLoader;

    import java.awt.*;
    import java.util.ArrayList;
    import java.util.List;

public class Player extends Entity {

    public static final int MAX_HEALTH = 3;
    public static final int MAX_ITEM_STACK = 5;

        public enum Facing { LEFT, RIGHT }
        private Facing facing = Facing.RIGHT;

        private int health = MAX_HEALTH;
        private int lastX, lastY;

        public int getLastX() { return lastX; }
        public int getLastY() { return lastY; }
        public void updateLastPosition() { this.lastX = x; this.lastY = y; }

        /** Turns remaining where the player is stuck in a sticky trap. */
        private int trappedTurns = 0;

        /** Brief invulnerability after taking a hit (in turns). */
        private int invincibleTurns = 0;

        private int campfireTurns = 0;
        private int lastRestedFloor = -1;

        public int getCampfireTurns() { return campfireTurns; }
        public void setCampfireTurns(int t) { this.campfireTurns = t; }
        public int getLastRestedFloor() { return lastRestedFloor; }
        public void setLastRestedFloor(int f) { this.lastRestedFloor = f; }

        private SmokeSpawner smokeSpawner;

        private List<Item> inventory = new ArrayList<>();
        private final Image heroImage;

        public Player(int x, int y) {
            super(x, y);
            heroImage = AssetLoader.get().hero;
            facing = Facing.RIGHT;
            this.lastX = x;
            this.lastY = y;
        }

        public Image getHeroImage() { return heroImage; }

        public Facing getFacing() { return facing; }
        public void setFacing(Facing facing) { this.facing = facing; }

        // -- Health ----------------------------------------------------

        public int getHealth()     { return health; }
        public boolean isDead()    { return health <= 0; }

        /** Reduces health by 1 unless invincible. Returns true if damage was applied. */
        public boolean takeDamage() {
            if (invincibleTurns > 0) return false;
            health--;
            invincibleTurns = 2; // 2-turn invincibility after each hit
            return true;
        }

        public boolean isInvincible() { return invincibleTurns > 0; }
        public void decrementInvincible() { if (invincibleTurns > 0) invincibleTurns--; }

        public void heal(int amount) {
            health = Math.min(MAX_HEALTH, health + amount);
        }

        public void resetHealth() { health = MAX_HEALTH; invincibleTurns = 0; }

        /**
         * Knocks the player one tile away from the attacker.
         * Picks the opposite direction; if blocked, tries perpendicular.
         */
        public void knockback(int attackerX, int attackerY, Map map) {
            int dx = Integer.signum(x - attackerX);
            int dy = Integer.signum(y - attackerY);

            // Handle same-tile collision (virtual direction didn't specify or wasn't provided)
            if (dx == 0 && dy == 0) {
                int[][] fallbackDirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
                for (int[] d : fallbackDirs) {
                    int nx = x + d[0];
                    int ny = y + d[1];
                    if (inBounds(nx, ny) && map.isPassable(nx, ny)) {
                        setPosition(nx, ny);
                        return;
                    }
                }
                return; // Completely cornered
            }

            // Try direct push-back first
            if (dx != 0 || dy != 0) {
                int nx = x + dx;
                int ny = y + dy;
                if (inBounds(nx, ny) && map.isPassable(nx, ny)) {
                    setPosition(nx, ny);
                    return;
                }
            }
            // Try perpendicular directions
            int[][] fallbacks = {{dy, -dx}, {-dy, dx}};
            for (int[] fb : fallbacks) {
                int nx = x + fb[0];
                int ny = y + fb[1];
                if (inBounds(nx, ny) && map.isPassable(nx, ny)) {
                    setPosition(nx, ny);
                    return;
                }
            }
            // Cornered -- no knockback, just take the damage in place
        }

        private boolean inBounds(int x, int y) {
            return x >= 0 && x < unseen.utils.Constants.GRID_WIDTH
                && y >= 0 && y < unseen.utils.Constants.GRID_HEIGHT;
        }

        // -- Inventory -------------------------------------------------

    public boolean addItem(Item item) {
        long heldCount = inventory.stream()
            .filter(existingItem -> existingItem.getClass().equals(item.getClass()))
            .count();
        if (heldCount >= MAX_ITEM_STACK) {
            return false;
        }
        inventory.add(item);
        return true;
    }
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

        public boolean isTrapped() { return trappedTurns > 0; }
        public void setTrapped(int turns) { this.trappedTurns = turns; }
        public void decrementTrapped() { if (trappedTurns > 0) trappedTurns--; }
    }
