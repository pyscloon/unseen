package unseen.ui.gamepanel;

import unseen.entities.Enemy;
import unseen.entities.PatrolEnemy;
import unseen.entities.Player;
import unseen.entities.SentryEnemy;
import unseen.game.ActiveFlare;
import unseen.game.GameState;
import unseen.game.Smoke;
import unseen.items.Flare;
import unseen.items.NoiseMaker;
import unseen.items.SmokeBomb;
import unseen.map.Tile;
import unseen.ui.GamePanel;
import unseen.utils.AssetLoader;
import unseen.utils.Constants;

import java.awt.*;
import java.util.List;

/**
 * Handles all rendering for the game panel, including the map, entities,
 * effects, HUD, and overlay screens.
 */
public class GameRenderer {

    private final GamePanel panel;
    private final LevelManager levelManager;

    public GameRenderer(GamePanel panel, LevelManager levelManager) {
        this.panel = panel;
        this.levelManager = levelManager;
    }

    /** Entry point called from {@link GamePanel#paintComponent}. */
    public void render(Graphics g) {
        drawMap(g);
        drawEntities(g);
        drawFlares(g);
        drawNoiseFlashes(g);
        if (panel.isTargetingNoiseMaker() || panel.isTargetingFlare())
            drawTargetingOverlay(g);

        if (panel.getGameState() == GameState.WIN) {
            // Dim overlay
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, panel.getWidth(), panel.getHeight());

            String line1 = "Floor " + levelManager.getFloorNumber() + " Complete!";
            g.setColor(Color.GREEN);
            g.setFont(new Font("Arial", Font.BOLD, 42));
            int w1 = g.getFontMetrics().stringWidth(line1);
            g.drawString(line1, (panel.getWidth() - w1) / 2, panel.getHeight() / 2 - 30);

            String line2 = "Press any key for Floor " + (levelManager.getFloorNumber() + 1);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 22));
            int w2 = g.getFontMetrics().stringWidth(line2);
            g.drawString(line2, (panel.getWidth() - w2) / 2, panel.getHeight() / 2 + 20);
        }

        if (panel.getGameState() == GameState.LOSE) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, panel.getWidth(), panel.getHeight());

            String line1 = "GAME OVER";
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            int lw1 = g.getFontMetrics().stringWidth(line1);
            g.drawString(line1, (panel.getWidth() - lw1) / 2, panel.getHeight() / 2 - 30);

            String line2 = "Reached Floor " + levelManager.getFloorNumber();
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            int lw2 = g.getFontMetrics().stringWidth(line2);
            g.drawString(line2, (panel.getWidth() - lw2) / 2, panel.getHeight() / 2 + 20);

            String line3 = "Press R to restart";
            g.setColor(new Color(200, 200, 200));
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            int lw3 = g.getFontMetrics().stringWidth(line3);
            g.drawString(line3, (panel.getWidth() - lw3) / 2, panel.getHeight() / 2 + 55);
        }

        if (panel.getGameState() == GameState.PAUSED) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, panel.getWidth(), panel.getHeight());

            String pauseText = "PAUSED";
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            int pw = g.getFontMetrics().stringWidth(pauseText);
            g.drawString(pauseText, (panel.getWidth() - pw) / 2, panel.getHeight() / 2 - 20);

            String resumeHint = "Press ESC or P to resume";
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            int rw = g.getFontMetrics().stringWidth(resumeHint);
            g.setColor(new Color(200, 200, 200));
            g.drawString(resumeHint, (panel.getWidth() - rw) / 2, panel.getHeight() / 2 + 25);
        }

        // Floor number in top-right corner
        if (panel.getGameState() == GameState.PLAYING) {
            String floorLabel = "Floor " + levelManager.getFloorNumber();
            g.setFont(new Font("Arial", Font.BOLD, 18));
            int lw = g.getFontMetrics().stringWidth(floorLabel);
            int rx = panel.getWidth() - lw - 14;
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRoundRect(rx - 6, 10, lw + 12, 26, 8, 8);
            g.setColor(new Color(255, 220, 100));
            g.drawString(floorLabel, rx, 28);

            // "TRAPPED!" indicator when the player is stuck in a sticky trap
            if (levelManager.getPlayer().isTrapped()) {
                String trapLabel = "TRAPPED!";
                g.setFont(new Font("Arial", Font.BOLD, 18));
                int tw = g.getFontMetrics().stringWidth(trapLabel);
                int tx = (panel.getWidth() - tw) / 2;
                g.setColor(new Color(0, 0, 0, 160));
                g.fillRoundRect(tx - 8, panel.getHeight() - 48, tw + 16, 28, 8, 8);
                g.setColor(new Color(220, 80, 20));
                g.drawString(trapLabel, tx, panel.getHeight() - 28);
            }
        }

        // Always draw inventory bar
        drawInventory(g);
    }

    // Draw the player's inventory at the top of the screen
    private void drawInventory(Graphics g) {
        int boxSize = 44;
        int spacing = 12;
        int slots = 3;
        int barWidth = slots * (boxSize + spacing) + 40;
        int barHeight = boxSize + 24;
        int panelWidth = panel.getWidth();
        int startX = (panelWidth - barWidth) / 2 + 10; // Centered horizontally
        int y = 18;
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(30, 30, 30, 180));
        g2.fillRoundRect(startX - 18, y - 14, barWidth, barHeight, 18, 18);
        // Draw label
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.setColor(new Color(220, 220, 220));
        // Draw NoiseMaker slot
        int x = startX;
        g2.setColor(new Color(255, 255, 180, 180));
        g2.setStroke(new java.awt.BasicStroke(3f));
        g2.drawRoundRect(x - 2, y - 2, boxSize + 4, boxSize + 4, 12, 12);
        g2.setColor(new Color(70, 70, 70, 220));
        g2.fillRoundRect(x, y, boxSize, boxSize, 12, 12);
        g2.setColor(new Color(180, 180, 180));
        g2.setStroke(new java.awt.BasicStroke(2f));
        g2.drawRoundRect(x, y, boxSize, boxSize, 12, 12);
        int iconPad = 6;
        // Only show NoiseMaker if in inventory
        Player player = levelManager.getPlayer();
        boolean hasNoiseMaker = player.getInventory().stream().anyMatch(i -> i instanceof NoiseMaker);
        if (hasNoiseMaker) {
            if (AssetLoader.get().noiseMaker != null) {
                g2.drawImage(AssetLoader.get().noiseMaker, x + iconPad, y + iconPad, boxSize - 2 * iconPad,
                        boxSize - 2 * iconPad, null);
            } else {
                g2.setColor(new Color(200, 180, 50));
                g2.fillOval(x + iconPad, y + iconPad, boxSize - 2 * iconPad, boxSize - 2 * iconPad);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("N", x + boxSize / 2 - 5, y + boxSize / 2 + 5);
            }
            // Draw small label above icon
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.setColor(new Color(220, 220, 180));
            int noiseLabelWidth = g2.getFontMetrics().stringWidth("Noise");
            g2.drawString("Noise", x + (boxSize - noiseLabelWidth) / 2, y - 6);
        }
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        g2.setColor(new Color(200, 200, 200, 180));
        g2.drawString("1", x + boxSize - 13, y + boxSize - 6);
        x += boxSize + spacing;
        // Draw SmokeBomb slot
        g2.setColor(new Color(200, 200, 255, 180));
        g2.setStroke(new java.awt.BasicStroke(3f));
        g2.drawRoundRect(x - 2, y - 2, boxSize + 4, boxSize + 4, 12, 12);
        g2.setColor(new Color(70, 70, 70, 220));
        g2.fillRoundRect(x, y, boxSize, boxSize, 12, 12);
        g2.setColor(new Color(180, 180, 180));
        g2.setStroke(new java.awt.BasicStroke(2f));
        g2.drawRoundRect(x, y, boxSize, boxSize, 12, 12);
        // Only show SmokeBomb if in inventory
        boolean hasSmokeBomb = player.getInventory().stream().anyMatch(i -> i instanceof SmokeBomb);
        if (hasSmokeBomb) {
            if (AssetLoader.get().smokeBomb != null) {
                g2.drawImage(AssetLoader.get().smokeBomb, x + iconPad, y + iconPad, boxSize - 2 * iconPad,
                        boxSize - 2 * iconPad, null);
            } else {
                g2.setColor(new Color(180, 180, 180));
                g2.fillOval(x + iconPad, y + iconPad, boxSize - 2 * iconPad, boxSize - 2 * iconPad);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("S", x + boxSize / 2 - 5, y + boxSize / 2 + 5);
            }
            // Draw small label above icon
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.setColor(new Color(200, 200, 255));
            int smokeLabelWidth = g2.getFontMetrics().stringWidth("Smoke");
            g2.drawString("Smoke", x + (boxSize - smokeLabelWidth) / 2, y - 6);
        }
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        g2.setColor(new Color(200, 200, 200, 180));
        g2.drawString("2", x + boxSize - 13, y + boxSize - 6);
        x += boxSize + spacing;

        // Draw Flare slot
        g2.setColor(new Color(255, 255, 180, 180));
        g2.setStroke(new java.awt.BasicStroke(3f));
        g2.drawRoundRect(x - 2, y - 2, boxSize + 4, boxSize + 4, 12, 12);
        g2.setColor(new Color(70, 70, 70, 220));
        g2.fillRoundRect(x, y, boxSize, boxSize, 12, 12);
        g2.setColor(new Color(180, 180, 180));
        g2.setStroke(new java.awt.BasicStroke(2f));
        g2.drawRoundRect(x, y, boxSize, boxSize, 12, 12);
        // Only show Flare if in inventory
        boolean hasFlare = player.getInventory().stream().anyMatch(i -> i instanceof Flare);
        if (hasFlare) {
            if (AssetLoader.get().lantern != null) {
                g2.drawImage(AssetLoader.get().lantern, x + iconPad, y + iconPad, boxSize - 2 * iconPad,
                        boxSize - 2 * iconPad, null);
            } else {
                g2.setColor(new Color(255, 255, 150));
                g2.fillOval(x + iconPad, y + iconPad, boxSize - 2 * iconPad, boxSize - 2 * iconPad);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("F", x + boxSize / 2 - 4, y + boxSize / 2 + 5);
            }
            // Draw small label above icon
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.setColor(new Color(255, 255, 180));
            int flareLabelWidth = g2.getFontMetrics().stringWidth("Lantern");
            g2.drawString("Lantern", x + (boxSize - flareLabelWidth) / 2, y - 6);
        }
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        g2.setColor(new Color(200, 200, 200, 180));
        g2.drawString("3", x + boxSize - 13, y + boxSize - 6);
    }

    private void drawMap(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;
        boolean[][] visible = levelManager.getVisible();
        float[][] lightLevel = levelManager.getLightLevel();
        List<Smoke> smokes = levelManager.getSmokes();

        // 1) draw base tiles
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                Tile tile = levelManager.getMap().getTile(x, y);
                int drawX = x * Constants.TILE_SIZE;
                int drawY = y * Constants.TILE_SIZE;
                switch (tile) {
                    case WALL:
                        if (AssetLoader.get().wall != null) {
                            g2.drawImage(AssetLoader.get().wall, drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE,
                                    null);
                        } else {
                            g2.setColor(new Color(90, 90, 90));
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        }
                        break;
                    case FLOOR:
                    case START:
                        if (AssetLoader.get().floor != null) {
                            // Deterministic flip variant per tile so the floor isn't repetitive.
                            // variant bits: bit0 = flipH, bit1 = flipV → 0=normal,1=H,2=V,3=HV
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
                            g2.drawImage(AssetLoader.get().floor, 0, 0, ts, ts, null);
                            g2.setTransform(saved);
                        } else {
                            g2.setColor(new Color(170, 170, 170));
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        }
                        break;
                    case TORCH:
                        if (AssetLoader.get().torch != null) {
                            g2.drawImage(AssetLoader.get().torch, drawX, drawY, Constants.TILE_SIZE,
                                    Constants.TILE_SIZE, null);
                        } else {
                            g2.setColor(new Color(255, 140, 0));
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        }
                        break;
                    case EXIT:
                        if (AssetLoader.get().nextFloor != null) {
                            g2.drawImage(AssetLoader.get().nextFloor, drawX, drawY, Constants.TILE_SIZE,
                                    Constants.TILE_SIZE, null);
                        } else {
                            g2.setColor(Color.YELLOW);
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        }
                        break;
                    default:
                        if (AssetLoader.get().floor != null) {
                            g2.drawImage(AssetLoader.get().floor, drawX, drawY, Constants.TILE_SIZE,
                                    Constants.TILE_SIZE, null);
                        } else {
                            g2.setColor(new Color(170, 170, 170));
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        }
                }
            }
        }

        // 2) draw items on ground (overlay inventory asset only)
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
                    }
                    // If image is missing, do not draw anything for the item.
                }
            }
        }

        // 3) draw sticky traps as brown X marks
        java.awt.Stroke savedStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int ts = Constants.TILE_SIZE;
        for (unseen.game.StickyTrap trap : levelManager.getTraps()) {
            int tx = trap.getX();
            int ty = trap.getY();
            if (!visible[ty][tx]) continue;
            int drawX = tx * ts;
            int drawY = ty * ts;
            int pad = ts / 5;
            g2.setColor(new Color(139, 80, 20, 210));
            g2.drawLine(drawX + pad, drawY + pad, drawX + ts - pad, drawY + ts - pad);
            g2.drawLine(drawX + ts - pad, drawY + pad, drawX + pad, drawY + ts - pad);
        }
        g2.setStroke(savedStroke);

        // 4) draw smoke clouds as semi-transparent filled circles with a gentle pulse
        savedStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        long smokeNow = System.currentTimeMillis();
        for (Smoke smoke : smokes) {
            int cx = smoke.getX() * Constants.TILE_SIZE + Constants.TILE_SIZE / 2;
            int cy = smoke.getY() * Constants.TILE_SIZE + Constants.TILE_SIZE / 2;
            // pixel radius covers all tiles within smoke.getRadius() grid tiles
            int pr = (int) ((smoke.getRadius() + 0.5f) * Constants.TILE_SIZE);
            // gentle alpha pulse between 120 and 170
            float pulse = (float) (0.5 + 0.5 * Math.sin(smokeNow / 450.0));
            int alpha = (int) (120 + 50 * pulse);
            // filled grey-green circle
            g2.setColor(new Color(100, 130, 100, alpha));
            g2.fillOval(cx - pr, cy - pr, pr * 2, pr * 2);
            // slightly darker border ring
            int borderAlpha = Math.min(255, alpha + 40);
            g2.setColor(new Color(60, 90, 60, borderAlpha));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawOval(cx - pr, cy - pr, pr * 2, pr * 2);
        }
        g2.setStroke(savedStroke);

        // 5) overlay fog (darkening) for tiles
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                if (!visible[y][x]) {
                    g2.setColor(new Color(0, 0, 0, 200)); // semi-transparent black
                    g2.fillRect(
                            x * Constants.TILE_SIZE,
                            y * Constants.TILE_SIZE,
                            Constants.TILE_SIZE,
                            Constants.TILE_SIZE);
                } else {
                    float light = lightLevel[y][x];
                    int alpha = (int) ((1.0f - light) * 200);
                    alpha = Math.max(0, Math.min(200, alpha));
                    if (alpha > 0) {
                        g2.setColor(new Color(0, 0, 0, alpha));
                        g2.fillRect(
                                x * Constants.TILE_SIZE,
                                y * Constants.TILE_SIZE,
                                Constants.TILE_SIZE,
                                Constants.TILE_SIZE);
                    }
                }
            }
        }
    }

    private void drawEntities(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean[][] visible = levelManager.getVisible();
        Player player = levelManager.getPlayer();

        // Player
        if (visible[player.getY()][player.getX()]) {
            if (player.getHeroImage() != null) {
                int scaledSize = Constants.TILE_SIZE * 2;
                int drawX = player.getX() * Constants.TILE_SIZE + (Constants.TILE_SIZE - scaledSize) / 2;
                int drawY = player.getY() * Constants.TILE_SIZE + (Constants.TILE_SIZE - scaledSize) / 2;
                if (player.getFacing() == Player.Facing.LEFT) {
                    ((Graphics2D) g).drawImage(player.getHeroImage(),
                            drawX + scaledSize,
                            drawY,
                            -scaledSize,
                            scaledSize,
                            null);
                } else {
                    g2.drawImage(player.getHeroImage(),
                            drawX,
                            drawY,
                            scaledSize,
                            scaledSize,
                            null);
                }
            } else {
                g2.setColor(Color.CYAN);
                g2.fillOval(
                        player.getX() * Constants.TILE_SIZE,
                        player.getY() * Constants.TILE_SIZE,
                        Constants.TILE_SIZE,
                        Constants.TILE_SIZE);
            }
        }

        // Enemies + chase arrows
        for (Enemy e : levelManager.getEnemies()) {

            int ex = e.getX();
            int ey = e.getY();

            // Draw enemy only if visible
            if (visible[ey][ex]) {
                java.awt.Image img = e.getEnemyImage();
                if (img != null) {
                    int ts = Constants.TILE_SIZE;
                    int spriteSize = (e instanceof PatrolEnemy)
                            ? (int) (ts * 0.75) // patrol is 75% of tile size
                            : ts;
                    int offset = (ts - spriteSize) / 2;
                    int drawX = ex * ts + offset;
                    int drawY = ey * ts + offset;
                    g2.drawImage(img, drawX, drawY, spriteSize, spriteSize, null);
                } else {
                    g2.setColor(Color.RED);
                    g2.fillOval(
                            ex * Constants.TILE_SIZE,
                            ey * Constants.TILE_SIZE,
                            Constants.TILE_SIZE,
                            Constants.TILE_SIZE);
                }

                // After drawing the enemy sprite (still inside visible[ey][ex] block)
                if (e instanceof SentryEnemy) {
                    SentryEnemy s = (SentryEnemy) e;
                    if (s.isAlertVisualActive()) {
                        // Draw a small circular badge with '!' above the enemy tile
                        int tileSize = Constants.TILE_SIZE;
                        int size = tileSize / 2; // badge size in px
                        int centerX = ex * tileSize + tileSize / 2;
                        int badgeX = centerX - size / 2;
                        int badgeY = ey * tileSize - (size / 2); // above the tile

                        // Clamp so badge doesn't draw off-screen
                        if (badgeY < 2)
                            badgeY = 2;

                        // Background circle (red)
                        g2.setColor(new Color(200, 40, 40, 220));
                        g2.fillOval(badgeX, badgeY, size, size);

                        // White border
                        g2.setColor(new Color(255, 255, 255, 200));
                        g2.setStroke(new BasicStroke(2f));
                        g2.drawOval(badgeX, badgeY, size, size);

                        // Draw '!' centered
                        String mark = "!";
                        // Choose a font size relative to badge
                        int fontSize = Math.max(10, size / 2 + 6);
                        Font oldFont = g2.getFont();
                        g2.setFont(new Font("Arial", Font.BOLD, fontSize));
                        FontMetrics fm = g2.getFontMetrics();
                        int tx = badgeX + (size - fm.stringWidth(mark)) / 2;
                        int ty = badgeY + (size - fm.getHeight()) / 2 + fm.getAscent();

                        g2.setColor(Color.WHITE);
                        g2.drawString(mark, tx, ty);

                        // restore font
                        g2.setFont(oldFont);
                    }
                }

                // Draw arrow only when:
                // - enemy is currently CHASE
                // - enemy actually has line-of-sight to the player right now
                // Draw faint dotted path preview
                if (e.getState() == Enemy.State.CHASE
                        && e.hasLineOfSightToPlayer(levelManager.getMap(), player, levelManager.getSmokes())) {

                    java.util.List<unseen.ai.Node> path = e.getPlannedPath(levelManager.getMap(), player);

                    if (path != null && path.size() > 1) {

                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setColor(new Color(255, 255, 120, 90)); // soft yellow, faint

                        for (int i = 1; i < path.size(); i++) {

                            unseen.ai.Node n = path.get(i);

                            // Only draw dots if tile is visible to player
                            if (visible[n.y][n.x]) {

                                int dotSize = Math.max(4, Constants.TILE_SIZE / 5);

                                int dx = n.x * Constants.TILE_SIZE
                                        + Constants.TILE_SIZE / 2
                                        - dotSize / 2;

                                int dy = n.y * Constants.TILE_SIZE
                                        + Constants.TILE_SIZE / 2
                                        - dotSize / 2;

                                g2d.fillOval(dx, dy, dotSize, dotSize);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Draw glowing auras around each active Flare.
     */
    private void drawFlares(Graphics g) {
        List<ActiveFlare> flares = levelManager.getFlares();
        if (flares.isEmpty())
            return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean[][] visible = levelManager.getVisible();
        long now = System.currentTimeMillis();
        int ts = Constants.TILE_SIZE;
        for (ActiveFlare f : flares) {
            // Only draw if within player FOV/visible
            if (!visible[f.getY()][f.getX()])
                continue;

            int cx = f.getX() * ts + ts / 2;
            int cy = f.getY() * ts + ts / 2;

            // 8-frame animation cycle based on system time (approx 1200ms total)
            // Frames: 0 (tiny square) -> 1 (small star) -> 2 (med star) -> 3 (large star)
            // -> 4 (expanding with detached particles) -> 5 (smaller core, particles
            // further) -> 6 (even smaller core, particles fading) -> 7 (tiny core,
            // particles gone)
            float animCycle = (float) ((now % 1200) / 1200.0);
            int frame = (int) (animCycle * 8);

            // Base glow aura
            int alpha = 40;
            if (frame >= 2 && frame <= 4)
                alpha = 60; // Brighter in middle frames
            int auraR = (int) (ts * 1.5f + (ts * 0.2f * (frame == 3 || frame == 4 ? 1.0 : 0.0)));
            g2.setColor(new Color(255, 255, 100, alpha));
            g2.fillOval(cx - auraR, cy - auraR, auraR * 2, auraR * 2);

            // Inner aura
            int innerR = (int) (ts * 0.8f);
            g2.setColor(new Color(255, 255, 180, alpha * 2));
            g2.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

            // Draw the star core based on the frame
            java.awt.geom.Path2D.Double star = new java.awt.geom.Path2D.Double();
            double r = 0;
            double innerCore = 0;

            if (frame == 0 || frame == 7) {
                // Tiny square/dot
                r = ts * 0.1;
                star.moveTo(cx - r, cy - r);
                star.lineTo(cx + r, cy - r);
                star.lineTo(cx + r, cy + r);
                star.lineTo(cx - r, cy + r);
                star.closePath();
            } else {
                // Determine star size
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
                } // Core shrinks slightly while detaching

                star.moveTo(cx, cy - r); // Top
                star.quadTo(cx + innerCore, cy - innerCore, cx + r, cy); // Right
                star.quadTo(cx + innerCore, cy + innerCore, cx, cy + r); // Bottom
                star.quadTo(cx - innerCore, cy + innerCore, cx - r, cy); // Left
                star.quadTo(cx - innerCore, cy - innerCore, cx, cy - r); // Top (to close the loop with a curve)
                star.closePath();
            }

            g2.setColor(new Color(255, 255, 220, 240));
            g2.fill(star);

            // Flakes/particles detaching in frames 4, 5, 6
            if (frame >= 4 && frame <= 6) {
                float partDist = 0;
                int pSize = 0;
                if (frame == 4) {
                    partDist = ts * 0.45f;
                    pSize = 3;
                } else if (frame == 5) {
                    partDist = ts * 0.65f;
                    pSize = 2;
                } else if (frame == 6) {
                    partDist = ts * 0.8f;
                    pSize = 1;
                }

                // Diagonal particles
                g2.fillOval(cx - (int) partDist - pSize, cy - (int) partDist - pSize, pSize * 2, pSize * 2); // Top-left
                g2.fillOval(cx + (int) partDist - pSize, cy - (int) partDist - pSize, pSize * 2, pSize * 2); // Top-right
                g2.fillOval(cx - (int) partDist - pSize, cy + (int) partDist - pSize, pSize * 2, pSize * 2); // Bottom-left
                g2.fillOval(cx + (int) partDist - pSize, cy + (int) partDist - pSize, pSize * 2, pSize * 2); // Bottom-right
            }
        }
    }

    /**
     * Draw expanding ripple rings at each active NoiseMaker flash position.
     * Two concentric orange/yellow rings radiate outward and fade over the
     * flash's lifetime. Drawn on top of everything so the player always sees
     * where the decoy was placed.
     */
    private void drawNoiseFlashes(Graphics g) {
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
            // Base alpha decreases as the flash ages (countdown 4 → 1)
            int baseAlpha = f.getCountdown() * 55; // 220, 165, 110, 55
            // Ring 1: inner ring, period 600 ms
            float t1 = (float) ((now % 600) / 600.0);
            int r1 = ts / 4 + (int) (ts * 0.9f * t1);
            int a1 = Math.min(255, (int) (baseAlpha * (1.0f - t1 * 0.5f)));
            g2.setColor(new Color(255, 210, 50, a1));
            g2.setStroke(new BasicStroke(3f));
            g2.drawOval(cx - r1, cy - r1, r1 * 2, r1 * 2);
            // Ring 2: outer ring, 200 ms behind ring 1
            float t2 = (float) (((now + 200) % 600) / 600.0);
            int r2 = ts / 4 + (int) (ts * 0.9f * t2);
            int a2 = Math.min(255, (int) (baseAlpha * (1.0f - t2 * 0.5f)));
            g2.setColor(new Color(255, 130, 20, a2));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - r2, cy - r2, r2 * 2, r2 * 2);
        }
        g2.setStroke(saved);
    }

    /**
     * Draws the NoiseMaker targeting overlay: dim, highlighted hover tile, and
     * instructions.
     */
    private void drawTargetingOverlay(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int ts = Constants.TILE_SIZE;

        // Slight dim over the whole map
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(0, 0, panel.getWidth(), panel.getHeight());

        int mouseGridX = panel.getMouseGridX();
        int mouseGridY = panel.getMouseGridY();

        // Highlight hovered tile
        boolean validTile = mouseGridX >= 0 && mouseGridY >= 0
                && mouseGridX < Constants.GRID_WIDTH && mouseGridY < Constants.GRID_HEIGHT
                && levelManager.getMap().isPassable(mouseGridX, mouseGridY);

        int tx = mouseGridX * ts;
        int ty = mouseGridY * ts;

        if (validTile) {
            if (panel.isTargetingFlare()) {
                g2.setColor(new Color(255, 240, 100, 130));
                g2.fillRect(tx, ty, ts, ts);
                g2.setColor(new Color(255, 255, 150, 220));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRect(tx, ty, ts, ts);

                // Crosshair lines
                int cx = tx + ts / 2, cy = ty + ts / 2;
                g2.setColor(new Color(255, 255, 150, 200));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(cx - ts / 2, cy, cx + ts / 2, cy);
                g2.drawLine(cx, cy - ts / 2, cx, cy + ts / 2);
            } else {
                g2.setColor(new Color(255, 200, 50, 130));
                g2.fillRect(tx, ty, ts, ts);
                g2.setColor(new Color(255, 220, 80, 220));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRect(tx, ty, ts, ts);

                // Crosshair lines
                int cx = tx + ts / 2, cy = ty + ts / 2;
                g2.setColor(new Color(255, 220, 80, 200));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(cx - ts / 2, cy, cx + ts / 2, cy);
                g2.drawLine(cx, cy - ts / 2, cx, cy + ts / 2);
            }
        } else if (mouseGridX >= 0 && mouseGridY >= 0
                && mouseGridX < Constants.GRID_WIDTH && mouseGridY < Constants.GRID_HEIGHT) {
            // Invalid tile — red tint
            g2.setColor(new Color(200, 50, 50, 100));
            g2.fillRect(tx, ty, ts, ts);
            g2.setColor(new Color(200, 80, 80, 180));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(tx, ty, ts, ts);
        }

        // Instruction banner at the bottom
        String actionName = panel.isTargetingFlare() ? "flare" : "noise";
        String msg = validTile ? "Click to throw " + actionName + "  |  Esc to cancel"
                : "Invalid tile  |  Esc to cancel";
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        int msgW = fm.stringWidth(msg);
        int bx = (panel.getWidth() - msgW) / 2 - 12;
        int by = panel.getHeight() - 42;
        g2.setColor(new Color(20, 20, 20, 190));
        g2.fillRoundRect(bx, by, msgW + 24, 28, 10, 10);

        // Colors for valid tile
        Color validColor;
        if (panel.isTargetingFlare()) {
            validColor = new Color(255, 255, 120);
        } else {
            validColor = new Color(255, 220, 80);
        }

        g2.setColor(validTile ? validColor : new Color(220, 100, 100));
        g2.drawString(msg, bx + 12, by + 20);
    }
}
