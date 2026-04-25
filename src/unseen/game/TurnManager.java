package unseen.game;

import unseen.entities.Enemy;
import unseen.entities.Player;
import unseen.map.Map;
import java.util.ArrayList;
import java.util.List;

public class TurnManager {

    public static GameState processTurn(
            Player player,
            List<Enemy> enemies,
            Map map,
            List<Smoke> smokes) {

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

            // Check for player capture — only living enemies can catch the player
            if (enemy.isAlive()
                    && enemy.getX() == player.getX()
                    && enemy.getY() == player.getY()) {

                return GameState.LOSE;
            }
        }

        // Remove all dead enemies from the live list so they stop rendering and taking turns
        enemies.removeIf(e -> !e.isAlive());

        if (map.getTile(player.getX(), player.getY())
                == unseen.map.Tile.EXIT) {

            return GameState.WIN;
        }

        return GameState.PLAYING;
    }
}