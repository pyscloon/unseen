package unseen.ui.gamepanel;

import unseen.entities.Enemy;
import unseen.entities.PatrolEnemy;
import unseen.entities.Player;
import unseen.entities.SentryEnemy;
import unseen.entities.CrawlerEnemy;
import unseen.game.ActiveFlare;
import unseen.game.Smoke;
import unseen.items.Flare;
import unseen.items.NoiseMaker;
import unseen.items.Shuriken;
import unseen.items.SmokeBomb;
import unseen.map.DecalType;
import unseen.map.Tile;
import unseen.ui.GamePanel;
import unseen.utils.AssetLoader;
import unseen.utils.Constants;

import java.awt.*;
import java.util.List;
import java.util.Random;

class WorldRenderer {
    private final GamePanel panel;
    private final LevelManager levelManager;

    WorldRenderer(GamePanel panel, LevelManager levelManager) {
        this.panel = panel;
        this.levelManager = levelManager;
    }

    void drawMap(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        boolean[][] visible = levelManager.getVisible();
        float[][] lightLevel = levelManager.getLightLevel();
        List<Smoke> smokes = levelManager.getSmokes();

        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                Tile tile = levelManager.getMap().getTile(x, y);
                int drawX = x * Constants.TILE_SIZE;
                int drawY = y * Constants.TILE_SIZE;
                switch (tile) {
                    case WALL:
                        Image wallImg = AssetLoader.get().wall;
                        if (wallImg != null)
                            g2.drawImage(wallImg, drawX, drawY,
                                    Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                        else {
                            g2.setColor(new Color(90, 90, 90));
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        }
                        break;
                    case FLOOR:
                    case START:
                        Image floorImg = AssetLoader.get().floor;
                        if (panel.isHorrorMode()) {
                            // 25% chance for bloody floors
                            Random tr = new Random(x * 31 + y * 17);
                            double roll = tr.nextDouble();
                            if (roll < 0.15 && AssetLoader.get().horrorFloor != null) {
                                floorImg = AssetLoader.get().horrorFloor;
                            } else if (roll < 0.25 && AssetLoader.get().dieTile != null) {
                                floorImg = AssetLoader.get().dieTile;
                            }
                        }

                        if (floorImg != null) {
                            int variant = (x * 31 + y * 17) & 3;
                            int ts = Constants.TILE_SIZE;
                            java.awt.geom.AffineTransform saved = g2.getTransform();
                            boolean flipH = (variant & 1) != 0;
                            boolean flipV = (variant & 2) != 0;
                            if (flipH) {
                                g2.translate(drawX + ts, drawY);
                                g2.scale(-1, 1);
                            } else {
                                g2.translate(drawX, drawY);
                            }
                            if (flipV) {
                                g2.translate(0, ts);
                                g2.scale(1, -1);
                            }
                            g2.drawImage(floorImg, 0, 0, ts, ts, null);
                            g2.setTransform(saved);
                        } else {
                            g2.setColor(new Color(170, 170, 170));
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        }
                        break;
                    case TORCH:
                        if (levelManager.getDarkEventTurns() <= 0) {
                            if (AssetLoader.get().torch != null)
                                g2.drawImage(AssetLoader.get().torch, drawX, drawY,
                                        Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                            else {
                                g2.setColor(new Color(255, 140, 0));
                                g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                            }
                        } else {
                            Image fallback = AssetLoader.get().floor;
                            if (panel.isHorrorMode()) {
                                Random tr = new Random(x * 31 + y * 17);
                                double roll = tr.nextDouble();
                                if (roll < 0.15 && AssetLoader.get().horrorFloor != null)
                                    fallback = AssetLoader.get().horrorFloor;
                                else if (roll < 0.25 && AssetLoader.get().dieTile != null)
                                    fallback = AssetLoader.get().dieTile;
                            }
                            if (fallback != null)
                                g2.drawImage(fallback, drawX, drawY,
                                        Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                        }
                        break;
                    case CAMPFIRE:
                        if (levelManager.getDarkEventTurns() <= 0) {
                            if (AssetLoader.get().campfire != null)
                                g2.drawImage(AssetLoader.get().campfire, drawX, drawY,
                                        Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                            else {
                                g2.setColor(new Color(255, 100, 0));
                                g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                            }
                        } else {
                            Image fallback = AssetLoader.get().floor;
                            if (panel.isHorrorMode()) {
                                Random tr = new Random(x * 31 + y * 17);
                                double roll = tr.nextDouble();
                                if (roll < 0.15 && AssetLoader.get().horrorFloor != null)
                                    fallback = AssetLoader.get().horrorFloor;
                                else if (roll < 0.25 && AssetLoader.get().dieTile != null)
                                    fallback = AssetLoader.get().dieTile;
                            }
                            if (fallback != null)
                                g2.drawImage(fallback, drawX, drawY,
                                        Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                        }
                        break;
                    case FAKE_EXIT:
                    case EXIT:
                        if (AssetLoader.get().nextFloor != null)
                            g2.drawImage(AssetLoader.get().nextFloor, drawX, drawY,
                                    Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                        else {
                            g2.setColor(Color.YELLOW);
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        }
                        break;

                    default:
                        Image defFloor = AssetLoader.get().floor;
                        if (panel.isHorrorMode()) {
                            Random tr = new Random(x * 31 + y * 17);
                            double roll = tr.nextDouble();
                            if (roll < 0.15 && AssetLoader.get().horrorFloor != null)
                                defFloor = AssetLoader.get().horrorFloor;
                            else if (roll < 0.25 && AssetLoader.get().dieTile != null)
                                defFloor = AssetLoader.get().dieTile;
                        }
                        if (defFloor != null)
                            g2.drawImage(defFloor, drawX, drawY,
                                    Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                        else {
                            g2.setColor(new Color(170, 170, 170));
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        }
                        break;
                }

                // Draw decals if visible
                if (visible[y][x]) {
                    unseen.map.DecalType decal = levelManager.getMap().getDecal(x, y);
                    if (decal != null) {
                        drawDecal(g2, x, y, decal);
                    }
                }
            }
        }

        // Items on ground
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                unseen.items.Item ground = levelManager.getMap().getItem(x, y);
                if (ground != null && visible[y][x]) {
                    int tx = x * Constants.TILE_SIZE;
                    int ty = y * Constants.TILE_SIZE;
                    int iconPad = 6;
                    if (ground instanceof NoiseMaker && AssetLoader.get().noiseMaker != null) {
                        g2.drawImage(AssetLoader.get().noiseMaker, tx + iconPad, ty + iconPad,
                                Constants.TILE_SIZE - 2 * iconPad, Constants.TILE_SIZE - 2 * iconPad, null);
                    } else if (ground instanceof SmokeBomb && AssetLoader.get().smokeBomb != null) {
                        g2.drawImage(AssetLoader.get().smokeBomb, tx + iconPad, ty + iconPad,
                                Constants.TILE_SIZE - 2 * iconPad, Constants.TILE_SIZE - 2 * iconPad, null);
                    } else if (ground instanceof Flare && AssetLoader.get().lantern != null) {
                        g2.drawImage(AssetLoader.get().lantern, tx + iconPad, ty + iconPad,
                                Constants.TILE_SIZE - 2 * iconPad, Constants.TILE_SIZE - 2 * iconPad, null);
                    } else if (ground instanceof Shuriken && AssetLoader.get().shuriken != null) {
                        g2.drawImage(AssetLoader.get().shuriken, tx + iconPad, ty + iconPad,
                                Constants.TILE_SIZE - 2 * iconPad, Constants.TILE_SIZE - 2 * iconPad, null);
                    } else if (ground instanceof unseen.items.Heart && AssetLoader.get().heart != null) {
                        int heartPad = 2; // smaller pad = bigger icon
                        g2.drawImage(AssetLoader.get().heart, tx + heartPad, ty + heartPad,
                                Constants.TILE_SIZE - 2 * heartPad, Constants.TILE_SIZE - 2 * heartPad, null);
                    }
                }
            }
        }

        // Sticky traps
        java.awt.Stroke savedStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int ts = Constants.TILE_SIZE;
        for (unseen.game.StickyTrap trap : levelManager.getTraps()) {
            int tx = trap.getX();
            int ty = trap.getY();
            if (!visible[ty][tx])
                continue;
            int drawX = tx * ts;
            int drawY = ty * ts;
            int pad = ts / 5;
            g2.setColor(new Color(139, 80, 20, 210));
            g2.drawLine(drawX + pad, drawY + pad, drawX + ts - pad, drawY + ts - pad);
            g2.drawLine(drawX + ts - pad, drawY + pad, drawX + pad, drawY + ts - pad);
        }
        g2.setStroke(savedStroke);

        // Smoke clouds
        savedStroke = g2.getStroke();
        long smokeNow = System.currentTimeMillis();
        for (Smoke smoke : smokes) {
            int cx = smoke.getX() * Constants.TILE_SIZE + Constants.TILE_SIZE / 2;
            int cy = smoke.getY() * Constants.TILE_SIZE + Constants.TILE_SIZE / 2;
            int pr = (int) ((smoke.getRadius() + 0.5f) * Constants.TILE_SIZE);
            float pulse = (float) (0.5 + 0.5 * Math.sin(smokeNow / 450.0));
            int alpha = (int) (120 + 50 * pulse);
            g2.setColor(new Color(100, 130, 100, alpha));
            g2.fillOval(cx - pr, cy - pr, pr * 2, pr * 2);
            int borderAlpha = Math.min(255, alpha + 40);
            g2.setColor(new Color(60, 90, 60, borderAlpha));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawOval(cx - pr, cy - pr, pr * 2, pr * 2);
        }
        g2.setStroke(savedStroke);

        // Fog overlay
        int defaultFogAlpha = panel.isHorrorMode() ? 220 : 200;
        int defaultMinAlpha = panel.isHorrorMode() ? 80 : 0;

        int baseFogAlpha = levelManager.getDarkEventTurns() > 0 ? 255 : defaultFogAlpha;
        int minLightAlpha = levelManager.getDarkEventTurns() > 0 ? 150 : defaultMinAlpha;

        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                if (!visible[y][x]) {
                    g2.setColor(new Color(0, 0, 0, baseFogAlpha));
                    g2.fillRect(x * Constants.TILE_SIZE, y * Constants.TILE_SIZE,
                            Constants.TILE_SIZE, Constants.TILE_SIZE);
                } else {
                    float light = lightLevel[y][x];
                    int alpha = (int) ((1.0f - light) * 200);
                    alpha = Math.max(minLightAlpha, Math.min(255, alpha));
                    if (alpha > 0) {
                        g2.setColor(new Color(0, 0, 0, alpha));
                        g2.fillRect(x * Constants.TILE_SIZE, y * Constants.TILE_SIZE,
                                Constants.TILE_SIZE, Constants.TILE_SIZE);
                    }
                }
            }
        }
    }


    void drawEntities(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean[][] visible = levelManager.getVisible();
        Player player = levelManager.getPlayer();

        boolean hidingPlayer = panel.isGrappling() && panel.getGrappleProgress() >= 0.4f;
        if (!hidingPlayer && visible[player.getY()][player.getX()]) {
            if (player.getHeroImage() != null) {
                int scaledSize = Constants.TILE_SIZE * 2;
                int drawX = player.getX() * Constants.TILE_SIZE + (Constants.TILE_SIZE - scaledSize) / 2;
                int drawY = player.getY() * Constants.TILE_SIZE + (Constants.TILE_SIZE - scaledSize) / 2;
                if (player.getFacing() == Player.Facing.LEFT) {
                    g2.drawImage(player.getHeroImage(),
                            drawX + scaledSize, drawY, -scaledSize, scaledSize, null);
                } else {
                    g2.drawImage(player.getHeroImage(),
                            drawX, drawY, scaledSize, scaledSize, null);
                }
            } else {
                g2.setColor(Color.CYAN);
                g2.fillOval(player.getX() * Constants.TILE_SIZE,
                        player.getY() * Constants.TILE_SIZE,
                        Constants.TILE_SIZE, Constants.TILE_SIZE);
            }
        }

        for (Enemy e : levelManager.getEnemies()) {
            int ex = e.getX();
            int ey = e.getY();
            if (!visible[ey][ex])
                continue;
            if (e instanceof unseen.entities.CrawlerEnemy) {
                unseen.entities.CrawlerEnemy crawler = (unseen.entities.CrawlerEnemy) e;
                int ts = Constants.TILE_SIZE;
                int tx = ex * ts;
                int ty = ey * ts;

                // -- Proximity danger ring (always visible when the tile is visible) --
                // Draws a faint pulsing ring to warn the player of its detection radius.
                long now2 = System.currentTimeMillis();
                float pulse2 = (float) (0.5 + 0.5 * Math.sin(now2 / 600.0));
                int ringRadius = (unseen.entities.CrawlerEnemy.getProximityRange() * ts)
                        + ts / 2; // half-tile buffer
                int alpha2 = crawler.isHunting()
                        ? (int) (160 + 60 * pulse2)   // bright red when chasing
                        : (int) (35 + 25 * pulse2);  // faint amber when wandering
                java.awt.Color ringColor = crawler.isHunting()
                        ? new java.awt.Color(220, 40, 40, alpha2)
                        : new java.awt.Color(180, 120, 40, alpha2);

                int ringCx = tx + ts / 2;
                int ringCy = ty + ts / 2;
                java.awt.Stroke savedForRing = g2.getStroke();
                g2.setColor(ringColor);
                g2.setStroke(new java.awt.BasicStroke(
                        crawler.isHunting() ? 2f : 1f,
                        java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND,
                        1f,
                        new float[]{4f, 4f},
                        (float) (now2 / 80.0 % 8)));   // animated dash offset
                g2.drawOval(ringCx - ringRadius, ringCy - ringRadius,
                        ringRadius * 2, ringRadius * 2);
                g2.setStroke(savedForRing);

                // -- Sprite (or fallback) ----------------------------------------
                java.awt.Image img2 = e.getEnemyImage();
                if (img2 != null) {
                    // Flip horizontally when moving left
                    if (e.getDirection() == unseen.entities.Enemy.Direction.LEFT) {
                        g2.drawImage(img2, tx + ts, ty, -ts, ts, null);
                    } else {
                        g2.drawImage(img2, tx, ty, ts, ts, null);
                    }
                } else {
                    // Fallback: dark brown oval with small red eye-dots
                    g2.setColor(new java.awt.Color(50, 28, 8, 220));
                    g2.fillOval(tx + 4, ty + 6, ts - 8, ts - 12);
                    g2.setColor(new java.awt.Color(180, 20, 20));
                    g2.fillOval(tx + 9, ty + 10, 5, 5);
                    g2.fillOval(tx + ts - 14, ty + 10, 5, 5);
                }

                // -- "HUNTING!" badge above sprite when chasing ------------------
                if (crawler.isHunting()) {
                    int badgeY = ty - 14;
                    if (badgeY < 2) badgeY = ty + ts + 2;
                    String badge = "!";
                    g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
                    java.awt.FontMetrics bfm = g2.getFontMetrics();
                    int bw = bfm.stringWidth(badge) + 10;
                    g2.setColor(new java.awt.Color(200, 30, 30, 210));
                    g2.fillOval(tx + ts / 2 - bw / 2, badgeY, bw, 18);
                    g2.setColor(java.awt.Color.WHITE);
                    g2.drawString(badge,
                            tx + ts / 2 - bfm.stringWidth(badge) / 2,
                            badgeY + bfm.getAscent() + 1);
                }

                continue; // skip the generic rendering below
            };
            if (e.getType() == Enemy.EnemyType.STALKER) {
                // Draw a terrifying shadowy glitch figure
                int ts = Constants.TILE_SIZE;
                int tx = ex * ts;
                int ty = ey * ts;
                Random rand = new Random();

                // Shadow body
                g2.setColor(new Color(0, 0, 0, 180 + rand.nextInt(75)));
                g2.fillRect(tx + 4, ty + 2, ts - 8, ts - 4);

                // Erratic shifting
                if (rand.nextDouble() < 0.2) {
                    g2.setColor(new Color(0, 0, 0, 100));
                    g2.fillRect(tx + rand.nextInt(20) - 10, ty, ts, ts);
                }

                // Piercing red glowing eyes
                g2.setColor(Color.RED);
                int eyeOff = rand.nextInt(3) - 1;
                g2.fillOval(tx + 10 + eyeOff, ty + 12, 6, 6);
                g2.fillOval(tx + 20 + eyeOff, ty + 12, 6, 6);

                // Glitch trails / static around it
                for (int i = 0; i < 4; i++) {
                    g2.setColor(new Color(255, 0, 0, 40));
                    g2.fillRect(tx + rand.nextInt(ts) - 5, ty + rand.nextInt(ts), rand.nextInt(15), 1);
                }
                continue;
            }

            java.awt.Image img = e.getEnemyImage();
            if (img != null) {
                int tileS = Constants.TILE_SIZE;
                int spriteSize = (e instanceof PatrolEnemy) ? (int) (tileS * 0.75) : tileS;
                int offset = (tileS - spriteSize) / 2;
                g2.drawImage(img, ex * tileS + offset, ey * tileS + offset, spriteSize, spriteSize, null);
            } else {
                g2.setColor(Color.RED);
                g2.fillOval(ex * Constants.TILE_SIZE, ey * Constants.TILE_SIZE,
                        Constants.TILE_SIZE, Constants.TILE_SIZE);
            }

            if (e instanceof SentryEnemy) {
                SentryEnemy s = (SentryEnemy) e;
                if (s.isAlertVisualActive()) {
                    int tileSize = Constants.TILE_SIZE;
                    int size = tileSize / 2;
                    int centerX = ex * tileSize + tileSize / 2;
                    int badgeX = centerX - size / 2;
                    int badgeY = ey * tileSize - (size / 2);
                    if (badgeY < 2)
                        badgeY = 2;

                    g2.setColor(new Color(200, 40, 40, 220));
                    g2.fillOval(badgeX, badgeY, size, size);
                    g2.setColor(new Color(255, 255, 255, 200));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(badgeX, badgeY, size, size);

                    String mark = "!";
                    int fontSize = Math.max(10, size / 2 + 6);
                    Font oldFont = g2.getFont();
                    g2.setFont(new Font("Arial", Font.BOLD, fontSize));
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = badgeX + (size - fm.stringWidth(mark)) / 2;
                    int ty = badgeY + (size - fm.getHeight()) / 2 + fm.getAscent();
                    g2.setColor(Color.WHITE);
                    g2.drawString(mark, tx, ty);
                    g2.setFont(oldFont);
                }
            }

            if (e.getState() == Enemy.State.CHASE
                    && e.hasLineOfSightToPlayer(levelManager.getMap(), player, levelManager.getSmokes())) {
                java.util.List<unseen.ai.Node> path = e.getPlannedPath(levelManager.getMap(), player);
                if (path != null && path.size() > 1) {
                    g2.setColor(new Color(255, 255, 120, 90));
                    for (int i = 1; i < path.size(); i++) {
                        unseen.ai.Node n = path.get(i);
                        if (visible[n.y][n.x]) {
                            int dotSize = Math.max(4, Constants.TILE_SIZE / 5);
                            int ddx = n.x * Constants.TILE_SIZE + Constants.TILE_SIZE / 2 - dotSize / 2;
                            int ddy = n.y * Constants.TILE_SIZE + Constants.TILE_SIZE / 2 - dotSize / 2;
                            g2.fillOval(ddx, ddy, dotSize, dotSize);
                        }
                    }
                }
            }
        }
        drawPhantom(g2);
    }


private void drawPhantom(Graphics2D g2) {
        int px = levelManager.getPhantomX();
        int py = levelManager.getPhantomY();
        if (px != -1) {
            Player player = levelManager.getPlayer();
            int ts = Constants.TILE_SIZE;

            java.awt.Composite saved = g2.getComposite();
            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.6f));

            if (player.getHeroImage() != null) {
                // Draw a desaturated/darker version of the player facing AWAY
                int scaledSize = ts * 2;
                int drawX = px * ts + (ts - scaledSize) / 2;
                int drawY = py * ts + (ts - scaledSize) / 2;

                // Always draw facing away (if player faces right, phantom faces left)
                if (player.getFacing() == Player.Facing.RIGHT) {
                    g2.drawImage(player.getHeroImage(), drawX + scaledSize, drawY, -scaledSize, scaledSize, null);
                } else {
                    g2.drawImage(player.getHeroImage(), drawX, drawY, scaledSize, scaledSize, null);
                }
            } else {
                // Fallback: simple dark silhouette
                g2.setColor(new Color(0, 0, 0, 180));
                g2.fillOval(px * ts + 8, py * ts + 4, ts - 16, ts - 8);
                g2.fillRect(px * ts + 6, py * ts + 12, ts - 12, ts - 12);
            }

            g2.setComposite(saved);
        }
    }


    void drawFlares(Graphics g) {
        List<ActiveFlare> flares = levelManager.getFlares();
        if (flares.isEmpty())
            return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean[][] visible = levelManager.getVisible();
        long now = System.currentTimeMillis();
        int ts = Constants.TILE_SIZE;
        for (ActiveFlare f : flares) {
            if (!visible[f.getY()][f.getX()])
                continue;
            int cx = f.getX() * ts + ts / 2;
            int cy = f.getY() * ts + ts / 2;

            float animCycle = (float) ((now % 1200) / 1200.0);
            int frame = (int) (animCycle * 8);

            int alpha = 40;
            if (frame >= 2 && frame <= 4)
                alpha = 60;
            int auraR = (int) (ts * 1.5f + (ts * 0.2f * (frame == 3 || frame == 4 ? 1.0 : 0.0)));
            g2.setColor(new Color(255, 255, 100, alpha));
            g2.fillOval(cx - auraR, cy - auraR, auraR * 2, auraR * 2);

            int innerR = (int) (ts * 0.8f);
            g2.setColor(new Color(255, 255, 180, alpha * 2));
            g2.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

            java.awt.geom.Path2D.Double star = new java.awt.geom.Path2D.Double();
            double r = 0, innerCore = 0;
            if (frame == 0 || frame == 7) {
                r = ts * 0.1;
                star.moveTo(cx - r, cy - r);
                star.lineTo(cx + r, cy - r);
                star.lineTo(cx + r, cy + r);
                star.lineTo(cx - r, cy + r);
                star.closePath();
            } else {
                if (frame == 1 || frame == 6) {
                    r = ts * 0.2;
                    innerCore = r * 0.15;
                } else if (frame == 2 || frame == 5) {
                    r = ts * 0.4;
                    innerCore = r * 0.15;
                } else if (frame == 3) {
                    r = ts * 0.6;
                    innerCore = r * 0.15;
                } else if (frame == 4) {
                    r = ts * 0.4;
                    innerCore = r * 0.15;
                }
                star.moveTo(cx, cy - r);
                star.quadTo(cx + innerCore, cy - innerCore, cx + r, cy);
                star.quadTo(cx + innerCore, cy + innerCore, cx, cy + r);
                star.quadTo(cx - innerCore, cy + innerCore, cx - r, cy);
                star.quadTo(cx - innerCore, cy - innerCore, cx, cy - r);
                star.closePath();
            }
            g2.setColor(new Color(255, 255, 220, 240));
            g2.fill(star);

            if (frame >= 4 && frame <= 6) {
                float partDist;
                int pSize;
                if (frame == 4) {
                    partDist = ts * 0.45f;
                    pSize = 3;
                } else if (frame == 5) {
                    partDist = ts * 0.65f;
                    pSize = 2;
                } else {
                    partDist = ts * 0.8f;
                    pSize = 1;
                }
                g2.fillOval(cx - (int) partDist - pSize, cy - (int) partDist - pSize, pSize * 2, pSize * 2);
                g2.fillOval(cx + (int) partDist - pSize, cy - (int) partDist - pSize, pSize * 2, pSize * 2);
                g2.fillOval(cx - (int) partDist - pSize, cy + (int) partDist - pSize, pSize * 2, pSize * 2);
                g2.fillOval(cx + (int) partDist - pSize, cy + (int) partDist - pSize, pSize * 2, pSize * 2);
            }
        }
    }


    void drawNoiseFlashes(Graphics g) {
        List<FlashEffect> noiseFlashes = levelManager.getNoiseFlashes();
        if (noiseFlashes.isEmpty())
            return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        java.awt.Stroke saved = g2.getStroke();
        long now = System.currentTimeMillis();
        int ts = Constants.TILE_SIZE;
        for (FlashEffect f : noiseFlashes) {
            int cx = f.getX() * ts + ts / 2;
            int cy = f.getY() * ts + ts / 2;
            if (f.isHoly()) {
                // --- HOLY PURIFICATION EFFECT (Smooth time-based) ---
                long elapsed = now - f.getStartTime();
                float progress = Math.min(1.0f, elapsed / 1000.0f); // 1 second full animation

                // Expand from 0 to 3 tiles radius
                int r = (int) (ts * 0.2f + ts * 2.8f * (float) Math.pow(progress, 0.5));
                // Fade out at the end
                int alpha = (int) (220 * (1.0f - progress));
                if (f.getCountdown() <= 1)
                    alpha *= 0.5; // Extra fade if almost gone

                if (alpha <= 0)
                    continue;

                // 1. Golden Aura
                g2.setColor(new Color(255, 255, 180, alpha / 3));
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);

                // 2. Expanding Cross
                g2.setColor(new Color(255, 240, 100, alpha));
                g2.setStroke(new BasicStroke(2f + 4f * (1.0f - progress)));
                int arm = r;
                g2.drawLine(cx - arm, cy, cx + arm, cy);
                g2.drawLine(cx, cy - arm, cx, cy + arm);

                // 3. Sparkles (using progress for movement)
                Random sparkleRand = new Random(f.getX() * 100 + f.getY());
                for (int i = 0; i < 8; i++) {
                    float sAngle = (float) (sparkleRand.nextDouble() * 2 * Math.PI);
                    float sDist = (float) (r * 0.2f + r * 0.8f * sparkleRand.nextDouble());
                    int sx = cx + (int) (Math.cos(sAngle) * sDist);
                    int sy = cy + (int) (Math.sin(sAngle) * sDist);

                    int sAlpha = (int) (alpha * (0.5 + 0.5 * Math.sin(now * 0.01 + i)));
                    g2.setColor(new Color(255, 255, 255, Math.max(0, sAlpha)));
                    g2.fillRect(sx, sy, 2, 2);
                }
            } else {
                // --- STANDARD NOISEMAKER RIPPLE ---
                int baseAlpha = Math.min(255, f.getCountdown() * 60);
                float t1 = (float) ((now % 600) / 600.0);
                int r1 = ts / 4 + (int) (ts * 0.9f * t1);
                int a1 = Math.min(baseAlpha, (int) (baseAlpha * (1.0f - t1)));
                g2.setColor(new Color(255, 210, 50, a1));
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(cx - r1, cy - r1, r1 * 2, r1 * 2);

                float t2 = (float) (((now + 200) % 600) / 600.0);
                int r2 = ts / 4 + (int) (ts * 0.9f * t2);
                int a2 = Math.min(baseAlpha, (int) (baseAlpha * (1.0f - t2)));
                g2.setColor(new Color(255, 130, 20, a2));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(cx - r2, cy - r2, r2 * 2, r2 * 2);
            }
        }
        g2.setStroke(saved);
    }


    void drawTileEffects(Graphics g) {
        List<TileEffect> effects = levelManager.getTileEffects();
        if (effects.isEmpty()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        java.awt.Stroke savedStroke = g2.getStroke();
        int ts = Constants.TILE_SIZE;

        java.util.Iterator<TileEffect> it = effects.iterator();
        while (it.hasNext()) {
            TileEffect effect = it.next();
            if (effect.isExpired()) {
                it.remove();
                continue;
            }

            float p = effect.getProgress();
            int cx = effect.getX() * ts + ts / 2;
            int cy = effect.getY() * ts + ts / 2;
            int alpha = Math.max(0, Math.min(220, (int) (220 * (1f - p))));
            int radius = (int) (ts * (0.25f + p * 1.15f));
            Color color = effect.getColor(alpha);

            g2.setColor(effect.getColor(Math.max(20, alpha / 4)));
            g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(effect.getKind() == TileEffect.Kind.PURIFY ? 4f : 2.5f));
            g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

            if (effect.getKind() == TileEffect.Kind.ALERT) {
                drawCenteredMark(g2, "!", cx, cy, color, 22);
            } else if (effect.getKind() == TileEffect.Kind.TRAP) {
                drawCenteredMark(g2, "X", cx, cy, color, 18);
            } else if (effect.getKind() == TileEffect.Kind.EXIT) {
                int arm = (int) (ts * (0.35f + p * 0.25f));
                g2.drawLine(cx - arm, cy, cx + arm, cy);
                g2.drawLine(cx, cy - arm, cx, cy + arm);
            } else if (effect.getKind() == TileEffect.Kind.PURIFY) {
                int arm = (int) (ts * (0.5f + p * 1.2f));
                g2.drawLine(cx - arm, cy, cx + arm, cy);
                g2.drawLine(cx, cy - arm, cx, cy + arm);
            }
        }

        g2.setStroke(savedStroke);
    }


private void drawCenteredMark(Graphics2D g2, String mark, int cx, int cy, Color color, int fontSize) {
        Font oldFont = g2.getFont();
        g2.setFont(new Font("Arial", Font.BOLD, fontSize));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(color);
        g2.drawString(mark, cx - fm.stringWidth(mark) / 2,
                cy - fm.getHeight() / 2 + fm.getAscent());
        g2.setFont(oldFont);
    }


    void drawShurikenProjectiles(Graphics g) {
        java.util.List<ShurikenProjectile> projs = panel.getShurikenProjectiles();
        if (projs.isEmpty())
            return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        java.awt.Image sprite = unseen.utils.AssetLoader.get().shuriken;
        int ts = Constants.TILE_SIZE;
        int spriteSize = (int) (ts * 0.8f); // slightly smaller than a tile

        java.util.Iterator<ShurikenProjectile> it = projs.iterator();
        while (it.hasNext()) {
            ShurikenProjectile proj = it.next();

            if (proj.isDone()) {
                it.remove();
                continue;
            }

            float[] pos = proj.getPixelPos();
            float cx = pos[0];
            float cy = pos[1];
            double angleDeg = proj.getAngleDeg();

            // -- Trail: 4 ghost copies stepping back along the direction -----------------
            int tdx = proj.getDx();
            int tdy = proj.getDy();
            float stepPx = ts * 0.35f; // spacing between ghost frames

            for (int ghost = 4; ghost >= 1; ghost--) {
                float gx = cx - tdx * stepPx * ghost;
                float gy = cy - tdy * stepPx * ghost;
                int alpha = (int) (28 * (5 - ghost)); // 28 -> 112, fades to tip

                java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
                at.translate(gx - spriteSize / 2.0, gy - spriteSize / 2.0);
                at.rotate(Math.toRadians(angleDeg - ghost * 15),
                        spriteSize / 2.0, spriteSize / 2.0);

                java.awt.Composite savedComp = g2.getComposite();
                g2.setComposite(java.awt.AlphaComposite.getInstance(
                        java.awt.AlphaComposite.SRC_OVER, alpha / 255f));

                if (sprite != null) {
                    g2.drawImage(sprite,
                            (int) (gx - spriteSize / 2f),
                            (int) (gy - spriteSize / 2f),
                            spriteSize, spriteSize, null);
                } else {
                    // Fallback: draw a small star shape
                    g2.setColor(new Color(180, 230, 255, alpha));
                    g2.fillOval((int) (gx - spriteSize / 2f), (int) (gy - spriteSize / 2f),
                            spriteSize, spriteSize);
                }
                g2.setComposite(savedComp);
            }

            // -- Main sprite -- rotated ------------------------------------------------
            java.awt.geom.AffineTransform saved = g2.getTransform();
            g2.translate(cx, cy);
            g2.rotate(Math.toRadians(angleDeg));
            g2.translate(-spriteSize / 2.0, -spriteSize / 2.0);

            if (sprite != null) {
                g2.drawImage(sprite, 0, 0, spriteSize, spriteSize, null);
            } else {
                // Fallback: bright circle + cross
                g2.setColor(new Color(200, 240, 255));
                g2.fillOval(0, 0, spriteSize, spriteSize);
                g2.setColor(new Color(80, 160, 220));
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(spriteSize / 2, 0, spriteSize / 2, spriteSize);
                g2.drawLine(0, spriteSize / 2, spriteSize, spriteSize / 2);
            }

            g2.setTransform(saved);

            // -- Impact flash at destination ------------------------------------------
            float progress = proj.getProgress();
            if (progress > 0.85f) {
                float fade = (progress - 0.85f) / 0.15f; // 0->1 as it arrives
                int flashAlpha = (int) (200 * fade);
                int destX = (proj.getOriginX() + proj.getDx() * proj.getTravelTiles()) * ts + ts / 2;
                int destY = (proj.getOriginY() + proj.getDy() * proj.getTravelTiles()) * ts + ts / 2;
                int flashR = (int) (ts * 0.6f * fade);
                java.awt.Composite savedComp = g2.getComposite();
                g2.setComposite(java.awt.AlphaComposite.getInstance(
                        java.awt.AlphaComposite.SRC_OVER, flashAlpha / 255f));
                g2.setColor(new Color(220, 240, 255));
                g2.fillOval(destX - flashR, destY - flashR, flashR * 2, flashR * 2);
                g2.setComposite(savedComp);
            }
        }
    }


    void drawGrappleAnimation(Graphics2D g2) {
        if (!panel.isGrappling())
            return;

        int ts = Constants.TILE_SIZE;
        float progress = panel.getGrappleProgress();

        // Start pixel position (center of start tile)
        int sx = panel.getGrappleStartX() * ts + ts / 2;
        int sy = panel.getGrappleStartY() * ts + ts / 2;

        // Wall pixel position (center of wall tile)
        int wx = panel.getGrappleWallX() * ts + ts / 2;
        int wy = panel.getGrappleWallY() * ts + ts / 2;

        // End pixel position (center of landing tile)
        int ex = panel.getGrappleEndX() * ts + ts / 2;
        int ey = panel.getGrappleEndY() * ts + ts / 2;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Two phases:
        // 0.0 -> 0.4: Hook flies to wall
        // 0.4 -> 1.0: Player zips to landing spot

        if (progress < 0.4f) {
            // PHASE 1: Hook flies out
            float hookP = progress / 0.4f;
            int hx = (int) (sx + (wx - sx) * hookP);
            int hy = (int) (sy + (wy - sy) * hookP);

            // Draw rope (thick cable)
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(100, 100, 110));
            g2.drawLine(sx, sy, hx, hy);
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(180, 180, 200));
            g2.drawLine(sx, sy, hx, hy);

            // Draw hook sprite at the tip
            Image hookImg = AssetLoader.get().grappleNoRope;
            if (hookImg == null)
                hookImg = AssetLoader.get().grapplingHook; // Fallback

            if (hookImg != null) {
                int sz = (int) (ts * 0.7);
                double angle = Math.atan2(wy - sy, wx - sx);
                java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
                at.translate(hx, hy);
                at.rotate(angle + Math.PI / 2); // Align top of sprite to direction
                at.translate(-sz / 2.0, -sz / 2.0);
                g2.drawImage(hookImg, at, null);
            }
        } else {
            // PHASE 2: Player zips
            float zipP = (progress - 0.4f) / 0.6f;
            int px = (int) (sx + (ex - sx) * zipP);
            int py = (int) (sy + (ey - sy) * zipP);

            // Draw rope from player to wall
            g2.setStroke(new BasicStroke(2.5f));
            g2.setColor(new Color(90, 90, 100));
            g2.drawLine(px, py, wx, wy);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(160, 160, 180));
            g2.drawLine(px, py, wx, wy);

            // Draw a motion blur/ghosting effect behind the zip
            int blurSteps = 3;
            for (int i = 1; i <= blurSteps; i++) {
                float trailP = Math.max(0, zipP - i * 0.05f);
                int tx = (int) (sx + (ex - sx) * trailP);
                int ty = (int) (sy + (ey - sy) * trailP);
                g2.setColor(new Color(150, 220, 255, 100 / i));
                g2.fillOval(tx - ts / 4, ty - ts / 4, ts / 2, ts / 2);
            }

            // Draw player sprite at moving position
            unseen.entities.Player player = levelManager.getPlayer();
            if (player.getHeroImage() != null) {
                int scaledSize = ts * 2;
                int dx = px - scaledSize / 2;
                int dy = py - scaledSize / 2;
                if (player.getFacing() == unseen.entities.Player.Facing.LEFT) {
                    g2.drawImage(player.getHeroImage(), dx + scaledSize, dy, -scaledSize, scaledSize, null);
                } else {
                    g2.drawImage(player.getHeroImage(), dx, dy, scaledSize, scaledSize, null);
                }
            }
        }
    }


private void drawDecal(Graphics2D g2, int gx, int gy, unseen.map.DecalType type) {
        int drawX = gx * Constants.TILE_SIZE;
        int drawY = gy * Constants.TILE_SIZE;
        Random r = new Random(gx * 31 + gy * 17);

        g2.setColor(new Color(140, 0, 0, 160)); // Dark dried blood

        switch (type) {
            case BLOOD_SPLATTER:
                for (int i = 0; i < 6; i++) {
                    int ox = r.nextInt(Constants.TILE_SIZE / 2);
                    int oy = r.nextInt(Constants.TILE_SIZE / 2);
                    int size = 4 + r.nextInt(12);
                    g2.fillOval(drawX + ox, drawY + oy, size, size);
                }
                break;
            case BLOODY_HANDPRINT:
                g2.fillOval(drawX + 12, drawY + 12, 10, 10); // palm
                for (int i = 0; i < 4; i++) {
                    g2.fillRect(drawX + 12 + i * 2, drawY + 4, 2, 8); // fingers
                }
                break;
            case BLOODY_TEXT_RUN:
                g2.setFont(new Font("Monospaced", Font.BOLD, 14));
                g2.drawString("RUN", drawX + 5, drawY + 25);
                break;
            case BLOODY_TEXT_HELP:
                g2.setFont(new Font("Monospaced", Font.BOLD, 14));
                g2.drawString("HELP", drawX + 5, drawY + 25);
                break;
            case BLOODY_TEXT_WATCHING:
                g2.setFont(new Font("Monospaced", Font.BOLD, 10));
                g2.drawString("WATCHING", drawX + 2, drawY + 20);
                break;
            case BLOODY_TEXT_HIDE:
                g2.setFont(new Font("Monospaced", Font.BOLD, 14));
                g2.drawString("HIDE", drawX + 5, drawY + 25);
                break;
            case BLOOD_TILE:
            case DIE_TILE:
                Image img = (type == DecalType.BLOOD_TILE)
                        ? unseen.utils.AssetLoader.get().horrorFloor
                        : unseen.utils.AssetLoader.get().dieTile;
                if (img != null) {
                    int variant = (gx * 31 + gy * 17) & 3;
                    int ts = Constants.TILE_SIZE;
                    java.awt.geom.AffineTransform saved = g2.getTransform();

                    // Center for rotation
                    g2.translate(drawX + ts / 2, drawY + ts / 2);

                    // Rotate based on variant
                    g2.rotate(Math.toRadians(variant * 90));

                    // Flip logic (optional, but adds more variety)
                    if ((variant & 1) != 0)
                        g2.scale(-1, 1);

                    g2.drawImage(img, -ts / 2, -ts / 2, ts, ts, null);
                    g2.setTransform(saved);
                }
                break;
            case NOTE_SCRAP:
                g2.setColor(new Color(240, 230, 200)); // Parchment color
                int nw = Constants.TILE_SIZE / 2;
                int nh = Constants.TILE_SIZE / 2;
                int nx = drawX + (Constants.TILE_SIZE - nw) / 2;
                int ny = drawY + (Constants.TILE_SIZE - nh) / 2;
                g2.fillRect(nx, ny, nw, nh);
                g2.setColor(new Color(100, 90, 70));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRect(nx, ny, nw, nh);
                // Draw some "scribbles"
                for (int i = 0; i < 3; i++) {
                    g2.drawLine(nx + 4, ny + 4 + i * 4, nx + nw - 4, ny + 4 + i * 4);
                }
                break;
            case PUDDLE:
                if (AssetLoader.get().puddle != null) {
                    g2.drawImage(AssetLoader.get().puddle, drawX, drawY,
                            Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                } else {
                    // Fallback to a translucent blue oval
                    g2.setColor(new Color(60, 120, 200, 120));
                    g2.fillOval(drawX + 4, drawY + 8, Constants.TILE_SIZE - 8, Constants.TILE_SIZE - 16);
                }
                break;
        }
    }


    void drawAtmosphericEffects(Graphics2D g2) {
        int w = panel.getWidth();
        int h = panel.getHeight();
        Random rand = new Random();
        long now = System.currentTimeMillis();

        // 1. COLD COLOR TINT (EERIE BLUE/GREEN)
        // This desaturates the scene and makes it feel colder.
        g2.setColor(new Color(20, 40, 60, 15));
        g2.fillRect(0, 0, w, h);

        // 2. FILM GRAIN / NOISE
        // Draw small random pixels of varying brightness to simulate ISO noise
        // Using a seed based on time so it 'flickers'
        Random flickerRand = new Random(now / 50);
        for (int i = 0; i < 1200; i++) {
            int rx = flickerRand.nextInt(w);
            int ry = flickerRand.nextInt(h);
            int bright = 120 + flickerRand.nextInt(135);
            g2.setColor(new Color(bright, bright, bright, 18));
            g2.fillRect(rx, ry, 1, 1);
        }

        // 3. SCANLINES (CRT EFFECT)
        g2.setColor(new Color(0, 0, 0, 12));
        for (int y = 0; y < h; y += 4) {
            g2.fillRect(0, y, w, 1);
        }

        // 4. FLOATING PARTICLES (ASH / DUST)
        // Moves slowly across the screen
        for (int i = 0; i < 15; i++) {
            int px = (int) ((i * 137 + now * 0.02) % w);
            int py = (int) ((i * 253 + now * 0.015) % h);
            int sz = 1 + (i % 3);
            g2.setColor(new Color(200, 200, 200, 40));
            g2.fillOval(px, py, sz, sz);
        }

        // 5. GLITCH ARTIFACTS (RARE)
        if (rand.nextDouble() < 0.02) {
            int gy = rand.nextInt(h);
            int gh = rand.nextInt(10) + 2;
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillRect(0, gy, w, gh);
            if (rand.nextBoolean()) {
                g2.setColor(new Color(255, 0, 0, 20));
                g2.fillRect(0, gy + gh, w, 1);
            }
        }
    }

}
