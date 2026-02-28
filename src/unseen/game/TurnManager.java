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

            enemy.takeTurn(map, player, smokes);

            // If enemy is a sentry, trigger alert system
            if (enemy instanceof unseen.entities.SentryEnemy) {
                ((unseen.entities.SentryEnemy) enemy)
                        .handleAlerts(enemies, player);
            }

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
