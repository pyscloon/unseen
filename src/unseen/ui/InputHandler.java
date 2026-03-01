package unseen.ui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import unseen.entities.Player;
import unseen.items.Item;
import unseen.items.NoiseMaker;
import unseen.items.SmokeBomb;
import unseen.map.Map;
import unseen.game.TurnManager;
import unseen.game.GameState;
import java.util.List;

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

        // Any key on the win screen advances to the next floor
        if (panel.getGameState() == GameState.WIN) {
            panel.nextFloor();
            return;
        }

        // Escape: cancel targeting or toggle pause
        if (key == KeyEvent.VK_ESCAPE) {
            if (panel.isTargetingNoiseMaker()) {
                panel.cancelTargeting();
            } else if (panel.getGameState() == GameState.PLAYING) {
                panel.pauseGame();
            } else if (panel.getGameState() == GameState.PAUSED) {
                panel.resumeGame();
            }
            return;
        }

        // P key also toggles pause
        if (key == KeyEvent.VK_P) {
            if (panel.getGameState() == GameState.PLAYING) {
                panel.pauseGame();
            } else if (panel.getGameState() == GameState.PAUSED) {
                panel.resumeGame();
            }
            return;
        }

        if (panel.getGameState() != GameState.PLAYING) return;

        // Any key while targeting (other than Escape/P above) also cancels
        if (panel.isTargetingNoiseMaker()) {
            panel.cancelTargeting();
            return;
        }

        Player player = panel.getPlayer();
        Map map = panel.getMap();

        int x = player.getX();
        int y = player.getY();
        boolean moved = false;

        // Item usage — find items by type so slot order doesn't matter after consumption
        if (key == KeyEvent.VK_1) {
            // Enter targeting mode: the player clicks where the noise should land
            boolean hasNoise = player.getInventory().stream().anyMatch(i -> i instanceof NoiseMaker);
            if (hasNoise) panel.enterNoiseMakerTargeting();
            return;
        }

        if (key == KeyEvent.VK_2) {
            List<Item> inv = player.getInventory();
            int idx = -1;
            for (int i = 0; i < inv.size(); i++) {
                if (inv.get(i) instanceof SmokeBomb) { idx = i; break; }
            }
            if (idx >= 0) {
                player.useItem(idx, map, panel.getEnemies());
                GameState result = TurnManager.processTurn(player, panel.getEnemies(), panel.getMap(), panel.getSmokes());
                panel.setGameState(result);
                panel.updateSmoke();
            }
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
            case KeyEvent.VK_W: y--; moved = true; break;
            case KeyEvent.VK_S: y++; moved = true; break;
            case KeyEvent.VK_A:
                if (x > 0 && map.isPassable(x - 1, y)) {
                    x--;
                    moved = true;
                } else {
                    // Look left if not moving
                    player.setFacing(Player.Facing.LEFT);
                    panel.repaint();
                    return;
                }
                break;
            case KeyEvent.VK_D:
                if (x < unseen.utils.Constants.GRID_WIDTH - 1 && map.isPassable(x + 1, y)) {
                    x++;
                    moved = true;
                } else {
                    // Look right if not moving
                    player.setFacing(Player.Facing.RIGHT);
                    panel.repaint();
                    return;
                }
                break;
            default: return;
        }

        if (moved && map.isPassable(x, y)) {
            // Set facing based on movement
            if (x < player.getX()) player.setFacing(Player.Facing.LEFT);
            else if (x > player.getX()) player.setFacing(Player.Facing.RIGHT);
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
