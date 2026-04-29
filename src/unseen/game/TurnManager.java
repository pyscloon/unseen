package unseen.game;

import unseen.entities.Enemy;
import unseen.entities.Player;
import unseen.map.Map;

import java.util.ArrayList;
import java.util.List;

public class TurnManager {
    private static final double PUDDLE_HEARING_RADIUS = 6.0;

    public static class TurnResult {
        public final GameState state;
        public final boolean playerHit;
        public final int killsThisTurn;

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

        List<Enemy> chasers = new ArrayList<>();
        for (Enemy e : enemies) {
            if (e.isAlive() && e.getState() == Enemy.State.CHASE) {
                chasers.add(e);
            }
        }

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

        player.decrementInvincible();

        for (Enemy enemy : new ArrayList<>(enemies)) {
            if (!enemy.isAlive()) {
                continue;
            }

            enemy.clearAttackedPlayerThisTurn();

            Enemy.State previousState = enemy.getState();
            enemy.takeTurn(map, player, smokes, enemies);
            boolean attackedPlayer = enemy.consumeAttackedPlayerThisTurn();

            if (panel != null
                    && previousState != Enemy.State.CHASE
                    && enemy.getState() == Enemy.State.CHASE) {
                panel.addTileEffect(enemy.getX(), enemy.getY(),
                        unseen.ui.gamepanel.TileEffect.Kind.ALERT);
            }

            if (enemy instanceof unseen.entities.SentryEnemy) {
                ((unseen.entities.SentryEnemy) enemy).handleAlerts(enemies, player);
            }

            if (enemy.isAlive()
                    && !(enemy instanceof unseen.entities.StalkerEnemy)
                    && (attackedPlayer
                    || (enemy.getX() == player.getX() && enemy.getY() == player.getY()))) {

                if (player.takeDamage()) {
                    wasHit = true;

                    int ex = enemy.getX();
                    int ey = enemy.getY();
                    int ax = ex;
                    int ay = ey;

                    if (ex == player.getX() && ey == player.getY()) {
                        switch (enemy.getDirection()) {
                            case UP:
                                ay++;
                                break;
                            case DOWN:
                                ay--;
                                break;
                            case LEFT:
                                ax++;
                                break;
                            case RIGHT:
                                ax--;
                                break;
                        }
                    }

                    player.knockback(ax, ay, map);

                    if (player.isDead()) {
                        int preDeathKills = (int) enemies.stream().filter(e -> !e.isAlive()).count();
                        enemies.removeIf(e -> !e.isAlive());
                        return new TurnResult(GameState.LOSE, true, preDeathKills);
                    }
                }
            }
        }

        int killsBefore = enemies.size();
        enemies.removeIf(e -> !e.isAlive());
        int killsThisTurn = killsBefore - enemies.size();

        boolean steppedOntoTile = player.getLastX() != player.getX()
                || player.getLastY() != player.getY();

        if (steppedOntoTile && map.getDecal(player.getX(), player.getY()) == unseen.map.DecalType.PUDDLE) {
            unseen.utils.SoundManager.get().play("splash", 1.0f);
            for (Enemy e : enemies) {
                double distanceToPuddle = Math.hypot(e.getX() - player.getX(), e.getY() - player.getY());
                if (distanceToPuddle <= PUDDLE_HEARING_RADIUS) {
                    e.redirectToNoise(player.getX(), player.getY());
                }
            }
            if (panel != null) {
                panel.addNoiseFlash(player.getX(), player.getY());
            }
        }

        if (map.getTile(player.getX(), player.getY()) == unseen.map.Tile.CAMPFIRE) {
            int currentFloor = (panel != null) ? panel.getLevelManager().getFloorNumber() : 0;
            int lastRested = player.getLastRestedFloor();
            boolean canRestOnThisFloor = (lastRested == -1 || (currentFloor - lastRested) >= 2);

            if (!steppedOntoTile && canRestOnThisFloor) {
                if (player.getHealth() < unseen.entities.Player.MAX_HEALTH) {
                    int campfireTurns = Math.max(0, player.getCampfireTurns()) + 1;
                    player.setCampfireTurns(campfireTurns);

                    if (campfireTurns >= 3) {
                        player.heal(1);
                        player.setCampfireTurns(0);
                        player.setLastRestedFloor(currentFloor);
                        if (panel != null) {
                            panel.showToast("Rested and Recovered! +1 HP", new java.awt.Color(255, 140, 40));
                        }
                    } else if (panel != null) {
                        panel.showToast("Resting... (" + player.getCampfireTurns() + "/3)",
                                new java.awt.Color(255, 180, 100));
                    }
                } else if (panel != null && player.getCampfireTurns() == 0) {
                    panel.showToast("You are already at full health.", java.awt.Color.WHITE);
                    player.setCampfireTurns(-1);
                }
            } else if (!steppedOntoTile && !canRestOnThisFloor) {
                if (panel != null && player.getCampfireTurns() == 0) {
                    panel.showToast("The fire is warm, but you've rested recently.", java.awt.Color.GRAY);
                    player.setCampfireTurns(-1);
                }
            } else if (steppedOntoTile) {
                if (player.getCampfireTurns() != 0) {
                    player.setCampfireTurns(0);
                }
                if (panel != null && canRestOnThisFloor) {
                    panel.showToast("Healing Sanctuary. Stand still to rest.", new java.awt.Color(100, 255, 100));
                }
            }
        } else if (player.getCampfireTurns() != 0) {
            player.setCampfireTurns(0);
        }

        player.updateLastPosition();

        if (panel != null) {
            panel.getLevelManager().checkNoteAt(player.getX(), player.getY());
        }

        if (map.getTile(player.getX(), player.getY()) == unseen.map.Tile.FAKE_EXIT) {
            map.setTile(player.getX(), player.getY(), unseen.map.Tile.FLOOR);
            if (panel != null) {
                panel.triggerFakeExit();
            }
            return new TurnResult(GameState.PLAYING, wasHit, killsThisTurn);
        }

        if (map.getTile(player.getX(), player.getY()) == unseen.map.Tile.EXIT) {
            if (panel != null) {
                panel.addTileEffect(player.getX(), player.getY(),
                        unseen.ui.gamepanel.TileEffect.Kind.EXIT);
            }
            return new TurnResult(GameState.WIN, wasHit, killsThisTurn);
        }

        return new TurnResult(GameState.PLAYING, wasHit, killsThisTurn);
    }

    public static GameState processTurn(
            unseen.ui.GamePanel panel,
            Player player,
            List<Enemy> enemies,
            Map map,
            List<Smoke> smokes) {
        return processTurnEx(panel, player, enemies, map, smokes).state;
    }
}
