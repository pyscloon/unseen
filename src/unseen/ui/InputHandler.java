package unseen.ui;

import unseen.entities.Player;
import unseen.game.GameState;
import unseen.items.*;
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
                panel.playUiClick();
                panel.getTutorial().nextPage();
                if (!panel.getTutorial().isActive()) {
                    panel.startFromMenu();
                }
            } else if (key == KeyEvent.VK_LEFT) {
                panel.playUiClick();
                panel.getTutorial().prevPage();
            } else if (key == KeyEvent.VK_ESCAPE) {
                panel.playUiClick();
                panel.getTutorial().dismiss();
            }
            panel.repaint();
            return;
        }

        if (panel.getGameState() == GameState.INTRO) {
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                panel.playUiClick();
                panel.advanceIntro();
            } else if (key == KeyEvent.VK_ESCAPE) {
                panel.playUiClick();
                panel.skipIntroToMenu();
            }
            return;
        }


        // Main menu controls
        if (panel.getGameState() == GameState.MENU) {
            if (panel.isAchievementsOpen()) {
                if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_A
                        || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                    panel.playUiClick();
                    panel.closeAchievements();
                    return;
                }
                return;
            }

            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                panel.playUiClick();
                panel.startFromMenu();
                return;
            }
            if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_Q) {
                panel.playUiClick();
                System.exit(0);
                return;
            }
            if (key == KeyEvent.VK_H) {
                panel.playUiClick();
                panel.getTutorial().reset();
                panel.repaint();
                return;
            }
            if (key == KeyEvent.VK_A) {
                panel.playUiClick();
                panel.openAchievements();
                return;
            }
            if (key == KeyEvent.VK_X) {
                panel.playHorrorToggleClick(!panel.isHorrorMode());
                panel.setHorrorMode(!panel.isHorrorMode());
                panel.repaint();
                return;
            }
            return;
        }


        // Retry after death
        if (panel.getGameState() == GameState.LOSE) {
            if (key == KeyEvent.VK_R) {
                panel.playUiClick();
                panel.restartGame();
                return;
            }
            if (key == KeyEvent.VK_ESCAPE) {
                panel.playUiClick();
                panel.returnToMenu();
                return;
            }
            return;
        }

        if (panel.getGameState() == GameState.REWARD_CHOICE) {
            if (key == KeyEvent.VK_ESCAPE) {
                panel.playUiClick();
                panel.returnToMenu();
                return;
            }
            if (key == KeyEvent.VK_1 || key == KeyEvent.VK_NUMPAD1) {
                panel.playUiClick();
                panel.chooseFloorReward(0);
                return;
            }
            if (key == KeyEvent.VK_2 || key == KeyEvent.VK_NUMPAD2) {
                panel.playUiClick();
                panel.chooseFloorReward(1);
                return;
            }
            if (key == KeyEvent.VK_3 || key == KeyEvent.VK_NUMPAD3) {
                panel.playUiClick();
                panel.chooseFloorReward(2);
                return;
            }
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                panel.playUiClick();
                panel.chooseFloorReward(0);
                return;
            }
            return;
        }


        // Win screen -- next floor or back to menu
        if (panel.getGameState() == GameState.WIN) {
            if (key == KeyEvent.VK_ESCAPE) {
                panel.playUiClick();
                panel.returnToMenu();
                return;
            }
            panel.playUiClick();
            panel.nextFloor();
            return;
        }

        // -- Confirm-quit overlay ------------------------------------------
        if (panel.getGameState() == GameState.CONFIRM_QUIT) {
            if (key == KeyEvent.VK_ESCAPE) {
                panel.playUiClick();
                panel.returnToMenu();
            } else {
                panel.playUiClick();
                // Any other key (M, P, space, ...) cancels back to PAUSED
                panel.setGameState(GameState.PAUSED);
                panel.repaint();
            }
            return;
        }

        // Pause state: any key to resume (except ESC/P which have toggle/menu roles)
        if (panel.getGameState() == GameState.PAUSED) {
            if (key == KeyEvent.VK_ESCAPE) {
                panel.playUiClick();
                panel.showQuitConfirm(); // ESC -> "Return to menu?" prompt
                return;
            } else if (key != KeyEvent.VK_P && key != KeyEvent.VK_R) {
                panel.playUiClick();
                panel.resumeGame();
                return;
            }
        }

        // Escape: cancel targeting -> pause
        if (key == KeyEvent.VK_ESCAPE) {
            if (panel.isTargetingNoiseMaker() || panel.isTargetingFlare()
                    || panel.isTargetingShuriken() || panel.isTargetingGrapplingHook()) {
                panel.playUiClick();
                panel.cancelTargeting();
            } else if (panel.getGameState() == GameState.PLAYING) {
                panel.playUiClick();
                panel.pauseGame();
            }
            return;
        }

        if (key == KeyEvent.VK_M) {
            panel.playUiClick();
            panel.toggleMusic();
            return;
        }

        if (key == KeyEvent.VK_N) {
            panel.playUiClick();
            unseen.utils.SoundManager.get().setSfxEnabled(!unseen.utils.SoundManager.get().isSfxEnabled());
            return;
        }

        if (key == KeyEvent.VK_P) {
            panel.playUiClick();
            if (panel.getGameState() == GameState.PLAYING)
                panel.pauseGame();
            else if (panel.getGameState() == GameState.PAUSED)
                panel.resumeGame();
            return;
        }

        if (key == KeyEvent.VK_R && panel.getGameState() == GameState.PAUSED) {
            panel.playUiClick();
            panel.restartGame();
            return;
        }

        if (key == KeyEvent.VK_V && panel.getGameState() == GameState.PLAYING) {
            panel.playUiClick();
            panel.toggleRoundQuestHud();
            return;
        }

        if (panel.getGameState() == GameState.PLAYING && panel.isShurikenInFlight()) {
            return;
        }

        if (panel.isTargetingNoiseMaker() || panel.isTargetingFlare() || panel.isTargetingGrapplingHook()) {
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
                    panel.playUiClick();
                    panel.confirmTargeting();
                    return;
            }
        }

        if (panel.isTargetingShuriken()) {
            switch (key) {
                case KeyEvent.VK_Q:
                    panel.setShurikenDirection(-1, -1);
                    return;
                case KeyEvent.VK_E:
                    panel.setShurikenDirection(1, -1);
                    return;
                case KeyEvent.VK_Z:
                    panel.setShurikenDirection(-1, 1);
                    return;
                case KeyEvent.VK_C:
                    panel.setShurikenDirection(1, 1);
                    return;
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
                    panel.playUiClick();
                    panel.confirmShurikenThrow();
                    return;
            }
            panel.playUiClick();
            panel.cancelTargeting();
            return;
        }

        if (panel.isTargetingNoiseMaker() || panel.isTargetingFlare() || panel.isTargetingGrapplingHook()) {
            panel.playUiClick();
            panel.cancelTargeting();
            return;
        }

        Player player = panel.getPlayer();
        Map map = panel.getMap();

        // -- Item keys -----------------------------------------------------

        if (key == KeyEvent.VK_1) {
            boolean hasNoise = player.getInventory().stream().anyMatch(i -> i instanceof NoiseMaker);
            if (hasNoise) {
                panel.playUiClick();
                panel.enterNoiseMakerTargeting();
            }
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
                panel.playUiClick();
                player.useItem(idx, map, panel.getEnemies());
                panel.processTurnAndApply();
            }
            return;
        }

        if (key == KeyEvent.VK_3) {
            boolean hasFlare = player.getInventory().stream().anyMatch(i -> i instanceof unseen.items.Flare);
            if (hasFlare) {
                panel.playUiClick();
                panel.enterFlareTargeting();
            }
            return;
        }

        if (key == KeyEvent.VK_4) {
            boolean hasShuriken = player.getInventory().stream().anyMatch(i -> i instanceof Shuriken);
            if (hasShuriken) {
                panel.playUiClick();
                panel.enterShurikenTargeting();
            }
            return;
        }

        if (key == KeyEvent.VK_5) {
            boolean hasHook = player.getInventory().stream().anyMatch(i -> i instanceof GrapplingHook);
            if (hasHook) {
                panel.playUiClick();
                panel.enterGrapplingHookTargeting();
            }
            return;
        }

        if (key == KeyEvent.VK_6 && panel.isHorrorMode()) {
            List<Item> inv = player.getInventory();
            int idx = -1;
            for (int i = 0; i < inv.size(); i++) {
                if (inv.get(i) instanceof unseen.items.Cross) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                panel.playUiClick();
                player.useItem(idx, map, panel.getEnemies());
                panel.processTurnAndApply();
            }
            return;
        }

        if (key == KeyEvent.VK_E) {
            if (panel.interactWithBarrel()) {
                return;
            }
            boolean picked = panel.attemptPickup();
            if (!picked)
                panel.showToast("Nothing to pick up here", new java.awt.Color(160, 160, 170));
            return;
        }

        // -- Wait / skip turn ----------------------------------------------

        if (key == KeyEvent.VK_SPACE) {
            panel.processTurnAndApply();
            panel.repaint();
            return;
        }

        // -- Trapped -- struggle free --------------------------------------

        if (isMovementKey(key) && player.isTrapped()) {
            player.decrementTrapped();
            panel.processTurnAndApply();
            return;
        }

        // -- Movement ------------------------------------------------------

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
            if (player.isHiddenInBarrel()) {
                player.setHiddenInBarrel(false);
                unseen.utils.SoundManager.get().play("ladder", 0.45f);
            }
            player.setPosition(x, y);

            final int movedX = x, movedY = y;
            panel.getTraps().removeIf(trap -> {
                if (trap.getX() == movedX && trap.getY() == movedY) {
                    player.setTrapped(1);
                    panel.addTileEffect(movedX, movedY, unseen.ui.gamepanel.TileEffect.Kind.TRAP);
                    panel.triggerShake(8, 3f);
                    panel.showToast("Sticky trap!", new java.awt.Color(255, 130, 40));
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
