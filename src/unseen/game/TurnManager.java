package unseen.game;

import unseen.entities.Enemy;
import unseen.entities.Player;
import unseen.map.Map;
import java.util.List;

public class TurnManager {

    public static GameState processTurn(
            Player player,
            List<Enemy> enemies,
            Map map,
            List<Smoke> smokes) { // add smoke list

        // Enemies move after player
        for (Enemy enemy : enemies) {

            // Each enemy executes its logic based on current world state
            enemy.takeTurn(map, player, smokes); // pass smokes

            // If this enemy is a sentry, let it alert nearby enemies (and set its visual)
            if (enemy instanceof unseen.entities.SentryEnemy) {
                ((unseen.entities.SentryEnemy) enemy).handleAlerts(enemies, player);
            }
            // tickAlertVisual is now called at the start of SentryEnemy.takeTurn() each turn

            // Check for player capture
            if (enemy.getX() == player.getX()
                    && enemy.getY() == player.getY()) {

                return GameState.LOSE;
            }
        }

        if (map.getTile(player.getX(), player.getY())
                == unseen.map.Tile.EXIT) {

            return GameState.WIN;
        }

        return GameState.PLAYING;
    }
}
