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

        public enum Facing { LEFT, RIGHT }
        private Facing facing = Facing.RIGHT;

        private int health = MAX_HEALTH;

        /** Turns remaining where the player is stuck in a sticky trap. */
        private int trappedTurns = 0;

        /** Brief invulnerability after taking a hit (in turns). */
        private int invincibleTurns = 0;

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

        // ── Health ────────────────────────────────────────────────────────────

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

        public void resetHealth() { health = MAX_HEALTH; invincibleTurns = 0; }

        /**
         * Knocks the player one tile away from the attacker.
         * Picks the opposite direction; if blocked, tries perpendicular.
         */
        public void knockback(int attackerX, int attackerY, Map map) {
            int dx = Integer.signum(x - attackerX);
            int dy = Integer.signum(y - attackerY);

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
            // Cornered — no knockback, just take the damage in place
        }

        private boolean inBounds(int x, int y) {
            return x >= 0 && x < unseen.utils.Constants.GRID_WIDTH
                && y >= 0 && y < unseen.utils.Constants.GRID_HEIGHT;
        }

        // ── Inventory ─────────────────────────────────────────────────────────

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

        public boolean isTrapped() { return trappedTurns > 0; }
        public void setTrapped(int turns) { this.trappedTurns = turns; }
        public void decrementTrapped() { if (trappedTurns > 0) trappedTurns--; }
    }
