package unseen.ui;

import unseen.entities.Player;
import unseen.game.GameState;
import unseen.game.TurnManager;
import unseen.items.Item;
import unseen.items.NoiseMaker;
import unseen.items.Shuriken;
import unseen.items.SmokeBomb;
import unseen.map.Map;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

public class InputHandler extends KeyAdapter {

    private GamePanel panel;

    public InputHandler(GamePanel panel) {
        this.panel = panel;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // Tutorial navigation intercepts all keys while open
        if (panel.getTutorial().isActive()) {
            if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                panel.getTutorial().nextPage();
                if (!panel.getTutorial().isActive()) {
                    panel.startFromMenu();
                }
            } else if (key == KeyEvent.VK_LEFT) {
                panel.getTutorial().prevPage();
            } else if (key == KeyEvent.VK_ESCAPE) {
                panel.getTutorial().dismiss();
            }
            panel.repaint();
            return;
        }

        // Main menu controls
        if (panel.getGameState() == GameState.MENU) {
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                panel.startFromMenu();
                return;
            }
            if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_Q) {
                System.exit(0);
                return;
            }
            if (key == KeyEvent.VK_H) {
                panel.getTutorial().reset();
                panel.repaint();
                return;
            }
            if (key == KeyEvent.VK_X) {
                panel.setHorrorMode(!panel.isHorrorMode());
                panel.repaint();
                return;
            }
            return;
        }

        // Retry after death
        if (panel.getGameState() == GameState.LOSE) {
            if (key == KeyEvent.VK_R) {
                panel.restartGame();
                return;
            }
            if (key == KeyEvent.VK_ESCAPE) {
                panel.returnToMenu();
                return;
            }
            return;
        }

        // Win screen — next floor or back to menu
        if (panel.getGameState() == GameState.WIN) {
            if (key == KeyEvent.VK_ESCAPE) {
                panel.returnToMenu();
                return;
            }
            panel.nextFloor();
            return;
        }

        // ── Confirm-quit overlay ────────────────────────────────────────────────
        if (panel.getGameState() == GameState.CONFIRM_QUIT) {
            if (key == KeyEvent.VK_ESCAPE) {
                panel.returnToMenu();
            } else {
                // Any other key (M, P, space, ...) cancels back to PAUSED
                panel.setGameState(GameState.PAUSED);
                panel.repaint();
            }
            return;
        }

        // Pause state: any key to resume (except ESC/P which have toggle/menu roles)
        if (panel.getGameState() == GameState.PAUSED) {
            if (key == KeyEvent.VK_ESCAPE) {
                panel.showQuitConfirm(); // ESC -> "Return to menu?" prompt
                return;
            } else if (key != KeyEvent.VK_P && key != KeyEvent.VK_R) {
                panel.resumeGame();
                return;
            }
        }

        // Escape: cancel targeting -> pause
        if (key == KeyEvent.VK_ESCAPE) {
            if (panel.isTargetingNoiseMaker() || panel.isTargetingFlare() || panel.isTargetingShuriken()) {
                panel.cancelTargeting();
            } else if (panel.getGameState() == GameState.PLAYING) {
                panel.pauseGame();
            }
            return;
        }

        if (key == KeyEvent.VK_M) {
            panel.toggleMusic();
            return;
        }

        if (key == KeyEvent.VK_N) {
            unseen.utils.SoundManager.get().setSfxEnabled(!unseen.utils.SoundManager.get().isSfxEnabled());
            return;
        }

        if (key == KeyEvent.VK_P) {
            if (panel.getGameState() == GameState.PLAYING)
                panel.pauseGame();
            else if (panel.getGameState() == GameState.PAUSED)
                panel.resumeGame();
            return;
        }

        if (key == KeyEvent.VK_R && panel.getGameState() == GameState.PAUSED) {
            panel.restartGame();
            return;
        }

        if (panel.getGameState() != GameState.PLAYING)
            return;

        if (panel.isTargetingNoiseMaker() || panel.isTargetingFlare()) {
            switch (key) {
                case KeyEvent.VK_W:
                case KeyEvent.VK_UP:
                    panel.moveTarget(0, -1);
                    return;
                case KeyEvent.VK_S:
                case KeyEvent.VK_DOWN:
                    panel.moveTarget(0, 1);
                    return;
                case KeyEvent.VK_A:
                case KeyEvent.VK_LEFT:
                    panel.moveTarget(-1, 0);
                    return;
                case KeyEvent.VK_D:
                case KeyEvent.VK_RIGHT:
                    panel.moveTarget(1, 0);
                    return;
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE:
                    panel.confirmTargeting();
                    return;
            }
        }

        if (panel.isTargetingShuriken()) {
            switch (key) {
                case KeyEvent.VK_W:
                case KeyEvent.VK_UP:
                    panel.setShurikenDirection(0, -1);
                    return;
                case KeyEvent.VK_S:
                case KeyEvent.VK_DOWN:
                    panel.setShurikenDirection(0, 1);
                    return;
                case KeyEvent.VK_A:
                case KeyEvent.VK_LEFT:
                    panel.setShurikenDirection(-1, 0);
                    return;
                case KeyEvent.VK_D:
                case KeyEvent.VK_RIGHT:
                    panel.setShurikenDirection(1, 0);
                    return;
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE:
                    panel.confirmShurikenThrow();
                    return;
            }
            panel.cancelTargeting();
            return;
        }

        if (panel.isTargetingNoiseMaker() || panel.isTargetingFlare()) {
            panel.cancelTargeting();
            return;
        }

        Player player = panel.getPlayer();
        Map map = panel.getMap();

        // ── Item keys ────────────────────────────────────────────────────────

        if (key == KeyEvent.VK_1) {
            boolean hasNoise = player.getInventory().stream().anyMatch(i -> i instanceof NoiseMaker);
            if (hasNoise)
                panel.enterNoiseMakerTargeting();
            return;
        }

        if (key == KeyEvent.VK_2) {
            List<Item> inv = player.getInventory();
            int idx = -1;
            for (int i = 0; i < inv.size(); i++) {
                if (inv.get(i) instanceof SmokeBomb) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                player.useItem(idx, map, panel.getEnemies());
                panel.processTurnAndApply();
            }
            return;
        }

        if (key == KeyEvent.VK_3) {
            boolean hasFlare = player.getInventory().stream().anyMatch(i -> i instanceof unseen.items.Flare);
            if (hasFlare)
                panel.enterFlareTargeting();
            return;
        }

        if (key == KeyEvent.VK_4) {
            boolean hasShuriken = player.getInventory().stream().anyMatch(i -> i instanceof Shuriken);
            if (hasShuriken)
                panel.enterShurikenTargeting();
            return;
        }

        if (key == KeyEvent.VK_E) {
            boolean picked = panel.attemptPickup();
            if (!picked)
                panel.showToast("Nothing to pick up here", new java.awt.Color(160, 160, 170));
            return;
        }

        // ── Wait / skip turn ─────────────────────────────────────────────────

        if (key == KeyEvent.VK_SPACE) {
            panel.processTurnAndApply();
            panel.repaint();
            return;
        }

        // ── Trapped — struggle free ──────────────────────────────────────────

        if (isMovementKey(key) && player.isTrapped()) {
            player.decrementTrapped();
            panel.processTurnAndApply();
            return;
        }

        // ── Movement ─────────────────────────────────────────────────────────

        int x = player.getX(), y = player.getY();
        boolean moved = false;

        switch (key) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                y--;
                moved = true;
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                y++;
                moved = true;
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                if (x > 0 && map.isPassable(x - 1, y)) {
                    x--;
                    moved = true;
                } else {
                    player.setFacing(Player.Facing.LEFT);
                    panel.repaint();
                    return;
                }
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                if (x < unseen.utils.Constants.GRID_WIDTH - 1 && map.isPassable(x + 1, y)) {
                    x++;
                    moved = true;
                } else {
                    player.setFacing(Player.Facing.RIGHT);
                    panel.repaint();
                    return;
                }
                break;
            default:
                return;
        }

        if (moved && map.isPassable(x, y)) {
            if (x < player.getX())
                player.setFacing(Player.Facing.LEFT);
            else if (x > player.getX())
                player.setFacing(Player.Facing.RIGHT);
            player.setPosition(x, y);

            final int movedX = x, movedY = y;
            panel.getTraps().removeIf(trap -> {
                if (trap.getX() == movedX && trap.getY() == movedY) {
                    player.setTrapped(1);
                    return true;
                }
                return false;
            });

            unseen.utils.SoundManager.get().playRandom(0.4f, "footstep1", "footstep2");
            panel.processTurnAndApply();
        }
    }

    private static boolean isMovementKey(int key) {
        return key == KeyEvent.VK_W || key == KeyEvent.VK_S
                || key == KeyEvent.VK_A || key == KeyEvent.VK_D
                || key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN
                || key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT;
    }
}