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
        this.panel = panel; // always store panel
    }

    @Override
    public void keyPressed(KeyEvent e) {

        // BLOCK input if game over
        if (panel.getGameState() != GameState.PLAYING) return;

        Player player = panel.getPlayer();
        Map map = panel.getMap();

        int x = player.getX();
        int y = player.getY();

        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: y--; break;
            case KeyEvent.VK_S: y++; break;
            case KeyEvent.VK_A: x--; break;
            case KeyEvent.VK_D: x++; break;
            // add other actions if needed
            default: return;
        }

        // Move player only if tile is passable
        if (map.isPassable(x, y)) {
            player.setPosition(x, y);

            // Process enemy turns and update game state
            GameState result = TurnManager.processTurn(
                    player,
                    panel.getEnemies(),
                    map
            );

            panel.setGameState(result);
        }
    }
}
