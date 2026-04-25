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
                return;
            }

            // Retry after death
            if (panel.getGameState() == GameState.LOSE && key == KeyEvent.VK_R) {
                panel.restartGame();
                return;
            }

            // Win screen — next floor
            if (panel.getGameState() == GameState.WIN) {
                panel.nextFloor();
                return;
            }

            // Escape: cancel targeting or toggle pause
            if (key == KeyEvent.VK_ESCAPE) {
                if (panel.isTargetingNoiseMaker() || panel.isTargetingFlare() || panel.isTargetingShuriken()) {
                    panel.cancelTargeting();
                } else if (panel.getGameState() == GameState.PLAYING) {
                    panel.pauseGame();
                } else if (panel.getGameState() == GameState.PAUSED) {
                    panel.resumeGame();
                }
                return;
            }

            if (key == KeyEvent.VK_P) {
                if (panel.getGameState() == GameState.PLAYING)       panel.pauseGame();
                else if (panel.getGameState() == GameState.PAUSED)   panel.resumeGame();
                return;
            }

            if (panel.getGameState() != GameState.PLAYING) return;

            if (panel.isTargetingShuriken()) {
                switch (key) {
                    case KeyEvent.VK_W: panel.setShurikenDirection(0, -1); return;
                    case KeyEvent.VK_S: panel.setShurikenDirection(0,  1); return;
                    case KeyEvent.VK_A: panel.setShurikenDirection(-1, 0); return;
                    case KeyEvent.VK_D: panel.setShurikenDirection( 1, 0); return;
                    case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_SPACE: panel.confirmShurikenThrow(); return;
                }
                // Any other key cancels
                panel.cancelTargeting();
                return;
            }

            if (panel.isTargetingNoiseMaker() || panel.isTargetingFlare()) {
                panel.cancelTargeting();
                return;
            }

            Player player = panel.getPlayer();
            Map map       = panel.getMap();

            // ---- Item keys ----

            // 1 — NoiseMaker (enter targeting mode)
            if (key == KeyEvent.VK_1) {
                boolean hasNoise = player.getInventory().stream().anyMatch(i -> i instanceof NoiseMaker);
                if (hasNoise) panel.enterNoiseMakerTargeting();
                return;
            }

            // 2 — SmokeBomb (instant use)
            if (key == KeyEvent.VK_2) {
                List<Item> inv = player.getInventory();
                int idx = -1;
                for (int i = 0; i < inv.size(); i++) {
                    if (inv.get(i) instanceof SmokeBomb) { idx = i; break; }
                }
                if (idx >= 0) {
                    player.useItem(idx, map, panel.getEnemies());
                    GameState result = TurnManager.processTurn(player, panel.getEnemies(),
                            panel.getMap(), panel.getSmokes());
                    panel.setGameState(result);
                    panel.updateSmoke();
                }
                return;
            }

            // 3 — Flare (enter targeting mode)
            if (key == KeyEvent.VK_3) {
                boolean hasFlare = player.getInventory().stream().anyMatch(i -> i instanceof unseen.items.Flare);
                if (hasFlare) panel.enterFlareTargeting();
                return;
            }

            // 4 — Shuriken: find and throw one, spawn death puff if a kill happened
            if (key == KeyEvent.VK_4) {
                boolean hasShuriken = player.getInventory().stream().anyMatch(i -> i instanceof Shuriken);
                if (hasShuriken) panel.enterShurikenTargeting();
                return;
            }

            // E — Pickup
            if (key == KeyEvent.VK_E) {
                boolean picked = panel.attemptPickup();
                if (!picked) System.out.println("Nothing to pick up here.");
                return;
            }

            // Trapped: spend turn escaping instead of moving
            if (isMovementKey(key) && player.isTrapped()) {
                player.decrementTrapped();
                GameState result = TurnManager.processTurn(player, panel.getEnemies(),
                        panel.getMap(), panel.getSmokes());
                panel.setGameState(result);
                panel.updateSmoke();
                return;
            }



            // Movement
            int x = player.getX(), y = player.getY();
            boolean moved = false;

            switch (key) {
                case KeyEvent.VK_W: y--;    moved = true; break;
                case KeyEvent.VK_S: y++;    moved = true; break;
                case KeyEvent.VK_A:
                    if (x > 0 && map.isPassable(x - 1, y)) { x--; moved = true; }
                    else { player.setFacing(Player.Facing.LEFT); panel.repaint(); return; }
                    break;
                case KeyEvent.VK_D:
                    if (x < unseen.utils.Constants.GRID_WIDTH - 1 && map.isPassable(x + 1, y)) { x++; moved = true; }
                    else { player.setFacing(Player.Facing.RIGHT); panel.repaint(); return; }
                    break;
                default: return;
            }

            if (moved && map.isPassable(x, y)) {
                if (x < player.getX())      player.setFacing(Player.Facing.LEFT);
                else if (x > player.getX()) player.setFacing(Player.Facing.RIGHT);
                player.setPosition(x, y);

                final int movedX = x, movedY = y;
                panel.getTraps().removeIf(trap -> {
                    if (trap.getX() == movedX && trap.getY() == movedY) {
                        player.setTrapped(1);
                        return true;
                    }
                    return false;
                });

                GameState result = TurnManager.processTurn(player, panel.getEnemies(),
                        panel.getMap(), panel.getSmokes());
                panel.setGameState(result);
                panel.updateSmoke();
            }
        }

        private static boolean isMovementKey(int key) {
            return key == KeyEvent.VK_W || key == KeyEvent.VK_S
                    || key == KeyEvent.VK_A || key == KeyEvent.VK_D;
        }
    }