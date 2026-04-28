package unseen.game;

import unseen.entities.Enemy;
import unseen.entities.Player;
import unseen.map.Map;
import java.util.ArrayList;
import java.util.List;

public class TurnManager {

    /**
     * Result of a turn -- extends the simple state with damage metadata
     * so the caller can trigger visual feedback (shake, vignette, toast).
     */
    public static class TurnResult {
        public final GameState state;
        public final boolean  playerHit;   // true if the player took damage this turn
        public final int      killsThisTurn;

        public TurnResult(GameState state, boolean playerHit, int kills) {
            this.state = state;
            this.playerHit = playerHit;
            this.killsThisTurn = kills;
        }
    }

    public static TurnResult processTurnEx(
            unseen.ui.GamePanel panel,
            Player player,
            List<Enemy> enemies,
            Map map,
            List<Smoke> smokes) {

        boolean wasHit = false;

        // 1. Identify all enemies in CHASE mode to assign flanking roles
        List<Enemy> chasers = new ArrayList<>();
        for (Enemy e : enemies) {
            if (e.isAlive() && e.getState() == Enemy.State.CHASE) {
                chasers.add(e);
            }
        }

        // 2. Assign roles: closest stays direct, others flank
        if (chasers.size() > 1) {
            Enemy closest = null;
            double minDist = Double.MAX_VALUE;
            for (Enemy e : chasers) {
                double d = Math.hypot(e.getX() - player.getX(), e.getY() - player.getY());
                if (d < minDist) {
                    minDist = d;
                    closest = e;
                }
            }
            for (Enemy e : chasers) {
                e.setFlanker(e != closest);
            }
        } else if (chasers.size() == 1) {
            chasers.get(0).setFlanker(false);
        }

        // Tick down invincibility from last turn
        player.decrementInvincible();

        // Iterate over a copy so removing dead enemies mid-loop is safe
        for (Enemy enemy : new ArrayList<>(enemies)) {

            // Skip enemies that were killed earlier this turn
            if (!enemy.isAlive()) continue;

            // Each enemy executes its logic based on current world state
            enemy.takeTurn(map, player, smokes, enemies);

            // If this enemy is a sentry, let it alert nearby enemies
            if (enemy instanceof unseen.entities.SentryEnemy) {
                ((unseen.entities.SentryEnemy) enemy).handleAlerts(enemies, player);
            }

            // Check for player contact -- damage + knockback instead of instant death
            if (enemy.isAlive()
                    && enemy.getX() == player.getX()
                    && enemy.getY() == player.getY()
                    && !(enemy instanceof unseen.entities.StalkerEnemy)) {

                if (player.takeDamage()) {
                    wasHit = true;
                    if (panel != null && panel.isHorrorMode()) {
                        unseen.utils.SoundManager.get().play("blood_splatter", 0.8f);
                    } else {
                        unseen.utils.SoundManager.get().play("player_hit");
                    }
                    int ex = enemy.getX();
                    int ey = enemy.getY();
                    int ax = ex;
                    int ay = ey;

                    // If enemy and player are on same tile, infer push from enemy direction
                    if (ex == player.getX() && ey == player.getY()) {
                        switch (enemy.getDirection()) {
                            case UP:    ay++; break;
                            case DOWN:  ay--; break;
                            case LEFT:  ax++; break;
                            case RIGHT: ax--; break;
                        }
                    }
                    player.knockback(ax, ay, map);

                    if (player.isDead()) {
                        // Count kills before returning
                        int preDeathKills = (int) enemies.stream().filter(e -> !e.isAlive()).count();
                        enemies.removeIf(e -> !e.isAlive());
                        return new TurnResult(GameState.LOSE, true, preDeathKills);
                    }
                }
            }
        }

        // Remove all dead enemies from the live list
        int killsBefore = enemies.size();
        enemies.removeIf(e -> !e.isAlive());
        int killsThisTurn = killsBefore - enemies.size();

        player.updateLastPosition();
        panel.getLevelManager().checkNoteAt(player.getX(), player.getY());

        if (map.getTile(player.getX(), player.getY())
                == unseen.map.Tile.EXIT) {
            return new TurnResult(GameState.WIN, wasHit, killsThisTurn);
        }

        return new TurnResult(GameState.PLAYING, wasHit, killsThisTurn);
    }

    /** Legacy adapter -- existing callers that only check the GameState still compile. */
    public static GameState processTurn(
            unseen.ui.GamePanel panel,
            Player player,
            List<Enemy> enemies,
            Map map,
            List<Smoke> smokes) {
        return processTurnEx(panel, player, enemies, map, smokes).state;
    }
}