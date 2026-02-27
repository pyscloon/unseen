package unseen.ui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import unseen.entities.Player;
import unseen.map.Map;
import unseen.game.TurnManager;
import unseen.game.GameState;

public class InputHandler extends KeyAdapter {

    private GamePanel panel;

    public InputHandler(GamePanel panel) {
        this.panel = panel;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // Retry after death: Press R to restart
        if (panel.getGameState() == GameState.LOSE && key == KeyEvent.VK_R) {
            panel.restartGame();
            return;
        }

        if (panel.getGameState() != GameState.PLAYING) return;

        Player player = panel.getPlayer();
        Map map = panel.getMap();

        int x = player.getX();
        int y = player.getY();

        // Item usage
        if (key == KeyEvent.VK_1) {
            player.useItem(0, map, panel.getEnemies());
            // consuming a turn after using item
            GameState result = TurnManager.processTurn(player, panel.getEnemies(), panel.getMap(), panel.getSmokes());
            panel.setGameState(result);
            panel.updateSmoke();
            return;
        }

        if (key == KeyEvent.VK_2) {
            player.useItem(1, map, panel.getEnemies());
            GameState result = TurnManager.processTurn(player, panel.getEnemies(), panel.getMap(), panel.getSmokes());
            panel.setGameState(result);
            panel.updateSmoke();
            return;
        }

        // Pickup key: E
        if (key == KeyEvent.VK_E) {
            boolean picked = panel.attemptPickup();
            if (!picked) {
                System.out.println("Nothing to pick up here.");
            }
            return;
        }

        // Movement
        switch (key) {
            case KeyEvent.VK_W: y--; break;
            case KeyEvent.VK_S: y++; break;
            case KeyEvent.VK_A: x--; break;
            case KeyEvent.VK_D: x++; break;
            default: return;
        }

        if (map.isPassable(x, y)) {
            player.setPosition(x, y);

            GameState result = TurnManager.processTurn(
                    player,
                    panel.getEnemies(),
                    panel.getMap(),
                    panel.getSmokes()
            );

            panel.setGameState(result);
            panel.updateSmoke();
        }
    }
}
