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
import unseen.items.Shuriken;
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
        if (panel.getGameState() == GameState.MENU) {
            drawMainMenu(g);
            if (panel.getTutorial().isActive()) {
                panel.getTutorial().draw(g, panel.getWidth(), panel.getHeight());
            }
            return;
        }

        drawMap(g);
        drawEntities(g);
        drawFlares(g);
        drawNoiseFlashes(g);
        if (panel.isTargetingNoiseMaker() || panel.isTargetingFlare())
            drawTargetingOverlay(g);
        if (panel.isTargetingShuriken())
            drawShurikenAimOverlay(g);

        if (panel.getGameState() == GameState.WIN) {
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

            String line3 = "M  —  Return to Menu";
            g.setColor(new Color(180, 180, 180));
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            int w3 = g.getFontMetrics().stringWidth(line3);
            g.drawString(line3, (panel.getWidth() - w3) / 2, panel.getHeight() / 2 + 52);
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

            String line3 = "R  —  Restart     M  —  Main Menu";
            g.setColor(new Color(200, 200, 200));
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            int lw3 = g.getFontMetrics().stringWidth(line3);
            g.drawString(line3, (panel.getWidth() - lw3) / 2, panel.getHeight() / 2 + 57);
        }

        if (panel.getGameState() == GameState.PAUSED) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, panel.getWidth(), panel.getHeight());

            String pauseText = "PAUSED";
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            int pw = g.getFontMetrics().stringWidth(pauseText);
            g.drawString(pauseText, (panel.getWidth() - pw) / 2, panel.getHeight() / 2 - 20);

            String resumeHint = "P  —  Resume";
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            int rw = g.getFontMetrics().stringWidth(resumeHint);
            g.setColor(new Color(200, 200, 200));
            g.drawString(resumeHint, (panel.getWidth() - rw) / 2, panel.getHeight() / 2 + 25);

            String menuHint = "ESC  —  Return to Menu";
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            int mw = g.getFontMetrics().stringWidth(menuHint);
            g.setColor(new Color(160, 160, 160));
            g.drawString(menuHint, (panel.getWidth() - mw) / 2, panel.getHeight() / 2 + 55);
        }

        // ── Return-to-menu confirmation overlay ──────────────────────────────────
        if (panel.getGameState() == GameState.CONFIRM_QUIT) {
            // Darken everything behind
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, panel.getWidth(), panel.getHeight());

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Card background
            int cardW = 380, cardH = 160;
            int cardX = (panel.getWidth()  - cardW) / 2;
            int cardY = (panel.getHeight() - cardH) / 2;

            g2.setColor(new Color(16, 12, 8, 240));
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 16, 16);
            g2.setColor(new Color(90, 58, 18, 200));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(cardX, cardY, cardW, cardH, 16, 16);
            g2.setColor(new Color(50, 33, 10, 100));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(cardX + 5, cardY + 5, cardW - 10, cardH - 10, 12, 12);

            // Question
            String question = "Return to Main Menu?";
            g2.setFont(new Font("Serif", Font.BOLD, 22));
            FontMetrics qfm = g2.getFontMetrics();
            int qw = qfm.stringWidth(question);
            g2.setColor(new Color(220, 190, 100));
            g2.drawString(question, cardX + (cardW - qw) / 2, cardY + 52);

            // Separator line
            g2.setColor(new Color(90, 58, 18, 140));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawLine(cardX + 30, cardY + 66, cardX + cardW - 30, cardY + 66);

            // Options
            String yes  = "M  —  Yes, go to menu";
            String no   = "Any other key  —  Stay";
            g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
            FontMetrics ofm = g2.getFontMetrics();

            int yw = ofm.stringWidth(yes);
            g2.setColor(new Color(180, 240, 140));
            g2.drawString(yes,  cardX + (cardW - yw) / 2, cardY + 98);

            int nw = ofm.stringWidth(no);
            g2.setColor(new Color(180, 180, 180));
            g2.drawString(no, cardX + (cardW - nw) / 2, cardY + 126);
        }
        // ────────────────────────────────────────────────────────────────────────

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

            // "TRAPPED!" indicator
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

    private void drawMainMenu(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = panel.getWidth();
        int h = panel.getHeight();
        long now = System.currentTimeMillis();

        Graphics2D bg = (Graphics2D) g2.create();
        double panX = Math.sin(now / 5000.0) * 5.0;
        double panY = Math.cos(now / 6500.0) * 3.5;
        double zoom = 1.02 + 0.008 * Math.sin(now / 7200.0);
        bg.translate(panX - (w * (zoom - 1.0)) / 2.0, panY - (h * (zoom - 1.0)) / 2.0);
        bg.scale(zoom, zoom);
        drawMap(bg);
        drawMenuTorchFlicker(bg, now);
        bg.dispose();

        g2.setColor(new Color(0, 0, 0, 155));
        g2.fillRect(0, 0, w, h);

        java.awt.RadialGradientPaint vignette = new java.awt.RadialGradientPaint(
                w / 2f, h / 2f, Math.max(w, h) * 0.72f,
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 160)});
        g2.setPaint(vignette);
        g2.fillRect(0, 0, w, h);

        int cardW = w - 160;
        int cardH = h - 230;
        int cardX = (w - cardW) / 2;
        int cardY = (h - cardH) / 2;

        g2.setColor(new Color(14, 11, 9, 210));
        g2.fillRect(cardX, cardY, cardW, cardH);

        g2.setColor(new Color(90, 58, 18, 180));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(cardX, cardY, cardW, cardH);

        g2.setColor(new Color(50, 33, 10, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(cardX + 5, cardY + 5, cardW - 10, cardH - 10);

        int cs = 7;
        g2.setColor(new Color(110, 72, 22, 200));
        g2.fillRect(cardX - 2,              cardY - 2,              cs, cs);
        g2.fillRect(cardX + cardW - cs + 2, cardY - 2,              cs, cs);
        g2.fillRect(cardX - 2,              cardY + cardH - cs + 2, cs, cs);
        g2.fillRect(cardX + cardW - cs + 2, cardY + cardH - cs + 2, cs, cs);

        float pulse = (float) (0.5 + 0.5 * Math.sin(now / 820.0));
        String title = "UNSEEN";
        Font titleFont = new Font("Serif", Font.BOLD, 68);
        g2.setFont(titleFont);
        int tw = g2.getFontMetrics().stringWidth(title);
        int titleX = (w - tw) / 2;
        int titleY = cardY + 115;

        g2.setColor(new Color(0, 0, 0, 200));
        g2.drawString(title, titleX + 4, titleY + 4);

        int glowAlpha = 55 + (int) (55 * pulse);
        g2.setColor(new Color(200, 110, 20, glowAlpha));
        for (int r = 5; r >= 1; r--) {
            g2.drawString(title, titleX - r, titleY);
            g2.drawString(title, titleX + r, titleY);
            g2.drawString(title, titleX, titleY - r);
            g2.drawString(title, titleX, titleY + r);
        }

        g2.setColor(new Color(220, 170, 60));
        g2.drawString(title, titleX, titleY);

        int sepY = titleY + 18;
        int sepInset = 60;
        g2.setColor(new Color(90, 58, 18, 180));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(cardX + sepInset, sepY, cardX + cardW - sepInset, sepY);
        int dx = w / 2, dy = sepY;
        int ds = 5;
        int[] dpx = {dx, dx + ds, dx, dx - ds};
        int[] dpy = {dy - ds, dy, dy + ds, dy};
        g2.setColor(new Color(140, 90, 25, 220));
        g2.fillPolygon(dpx, dpy, 4);

        // ── Menu options — all uniform plain text style ──────────────────────────
        int optY = sepY + 52;

        String start = "SPACE  to  Start";
        g2.setFont(new Font("Serif", Font.BOLD, 22));
        int sw = g2.getFontMetrics().stringWidth(start);
        g2.setColor(new Color(0, 0, 0, 130));
        g2.drawString(start, (w - sw) / 2 + 1, optY + 1);
        g2.setColor(new Color(210, 175, 90));
        g2.drawString(start, (w - sw) / 2, optY);

        String howTo = "H : How to Play";
        g2.setFont(new Font("Serif", Font.PLAIN, 18));
        int hw2 = g2.getFontMetrics().stringWidth(howTo);
        g2.setColor(new Color(0, 0, 0, 130));
        g2.drawString(howTo, (w - hw2) / 2 + 1, optY + 39);
        g2.setColor(new Color(118, 100, 72));
        g2.drawString(howTo, (w - hw2) / 2, optY + 38);

        String quit = "Q : Quit";
        g2.setFont(new Font("Serif", Font.PLAIN, 18));
        int qw = g2.getFontMetrics().stringWidth(quit);
        g2.setColor(new Color(0, 0, 0, 130));
        g2.drawString(quit, (w - qw) / 2 + 1, optY + 76);
        g2.setColor(new Color(118, 100, 72));
        g2.drawString(quit, (w - qw) / 2, optY + 75);
        // ── end menu options ─────────────────────────────────────────────────────

        String hint = "WASD - Move     E - Pick up     1/2/3/4 - Items";
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        int hintW = g2.getFontMetrics().stringWidth(hint);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(hint, (w - hintW) / 2 + 1, h - 36);
        g2.setColor(new Color(210, 190, 140));
        g2.drawString(hint, (w - hintW) / 2, h - 37);

        String version = "v0.1 Alpha";
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        int vw = g2.getFontMetrics().stringWidth(version);
        g2.setColor(new Color(150, 135, 100));
        g2.drawString(version, w - vw - 12, h - 14);

        g2.dispose();
    }

    private void drawMenuTorchFlicker(Graphics2D g2, long now) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int ty = 0; ty < Constants.GRID_HEIGHT; ty++) {
            for (int tx = 0; tx < Constants.GRID_WIDTH; tx++) {
                if (levelManager.getMap().getTile(tx, ty) != Tile.TORCH) continue;

                float phase = (float) ((tx * 17 + ty * 23) * 0.21);
                float flicker = (float) (0.5 + 0.5 * Math.sin(now / 155.0 + phase));

                int cx = tx * Constants.TILE_SIZE + Constants.TILE_SIZE / 2;
                int cy = ty * Constants.TILE_SIZE + Constants.TILE_SIZE / 2;

                int outer = (int) (Constants.TILE_SIZE * (2.8 + 0.8 * flicker));
                int mid   = (int) (Constants.TILE_SIZE * (1.6 + 0.5 * flicker));
                int core  = (int) (Constants.TILE_SIZE * (0.7 + 0.2 * flicker));

                g2.setColor(new Color(160, 80, 10, 18 + (int) (14 * flicker)));
                g2.fillOval(cx - outer / 2, cy - outer / 2, outer, outer);

                g2.setColor(new Color(230, 130, 30, 28 + (int) (22 * flicker)));
                g2.fillOval(cx - mid / 2, cy - mid / 2, mid, mid);

                g2.setColor(new Color(255, 210, 100, 50 + (int) (30 * flicker)));
                g2.fillOval(cx - core / 2, cy - core / 2, core, core);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Inventory HUD — slots 1=NoiseMaker, 2=SmokeBomb, 3=Flare, 4=Shuriken
    // -------------------------------------------------------------------------
    private void drawInventory(Graphics g) {
        int boxSize = 44;
        int spacing = 12;
        int slots   = 4;
        int barWidth  = slots * (boxSize + spacing) + 40;
        int barHeight = boxSize + 24;
        int panelWidth = panel.getWidth();
        int startX = (panelWidth - barWidth) / 2 + 10;
        int y = 18;
        int iconPad = 6;

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(30, 30, 30, 180));
        g2.fillRoundRect(startX - 18, y - 14, barWidth, barHeight, 18, 18);

        Player player = levelManager.getPlayer();

        // Slot 1 — NoiseMaker
        int x = startX;
        drawSlotBox(g2, x, y, boxSize, new Color(255, 255, 180, 180));
        boolean hasNoiseMaker = player.getInventory().stream().anyMatch(i -> i instanceof NoiseMaker);
        if (hasNoiseMaker) {
            if (AssetLoader.get().noiseMaker != null)
                g2.drawImage(AssetLoader.get().noiseMaker, x + iconPad, y + iconPad,
                        boxSize - 2 * iconPad, boxSize - 2 * iconPad, null);
            else {
                g2.setColor(new Color(200, 180, 50));
                g2.fillOval(x + iconPad, y + iconPad, boxSize - 2 * iconPad, boxSize - 2 * iconPad);
            }
            drawSlotLabel(g2, "Noise", x, y, boxSize, new Color(220, 220, 180));
        }
        drawSlotNumber(g2, "1", x, y, boxSize);
        x += boxSize + spacing;

        // Slot 2 — SmokeBomb
        drawSlotBox(g2, x, y, boxSize, new Color(200, 200, 255, 180));
        boolean hasSmokeBomb = player.getInventory().stream().anyMatch(i -> i instanceof SmokeBomb);
        if (hasSmokeBomb) {
            if (AssetLoader.get().smokeBomb != null)
                g2.drawImage(AssetLoader.get().smokeBomb, x + iconPad, y + iconPad,
                        boxSize - 2 * iconPad, boxSize - 2 * iconPad, null);
            else {
                g2.setColor(new Color(180, 180, 180));
                g2.fillOval(x + iconPad, y + iconPad, boxSize - 2 * iconPad, boxSize - 2 * iconPad);
            }
            drawSlotLabel(g2, "Smoke", x, y, boxSize, new Color(200, 200, 255));
        }
        drawSlotNumber(g2, "2", x, y, boxSize);
        x += boxSize + spacing;

        // Slot 3 — Flare / Lantern
        drawSlotBox(g2, x, y, boxSize, new Color(255, 255, 180, 180));
        boolean hasFlare = player.getInventory().stream().anyMatch(i -> i instanceof Flare);
        if (hasFlare) {
            if (AssetLoader.get().lantern != null)
                g2.drawImage(AssetLoader.get().lantern, x + iconPad, y + iconPad,
                        boxSize - 2 * iconPad, boxSize - 2 * iconPad, null);
            else {
                g2.setColor(new Color(255, 255, 150));
                g2.fillOval(x + iconPad, y + iconPad, boxSize - 2 * iconPad, boxSize - 2 * iconPad);
            }
            drawSlotLabel(g2, "Lantern", x, y, boxSize, new Color(255, 255, 180));
        }
        drawSlotNumber(g2, "3", x, y, boxSize);
        x += boxSize + spacing;

        // Slot 4 — Shuriken
        drawSlotBox(g2, x, y, boxSize, new Color(180, 220, 255, 180));
        boolean hasShuriken = player.getInventory().stream().anyMatch(i -> i instanceof Shuriken);
        if (hasShuriken) {
            if (AssetLoader.get().shuriken != null)
                g2.drawImage(AssetLoader.get().shuriken, x + iconPad, y + iconPad,
                        boxSize - 2 * iconPad, boxSize - 2 * iconPad, null);
            else {
                g2.setColor(new Color(160, 200, 240));
                int cx2 = x + boxSize / 2, cy2 = y + boxSize / 2, r2 = boxSize / 2 - iconPad;
                g2.fillOval(cx2 - r2, cy2 - r2, r2 * 2, r2 * 2);
                g2.setColor(Color.DARK_GRAY);
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                g2.drawString("*", cx2 - 4, cy2 + 5);
            }
            drawSlotLabel(g2, "Shuriken", x, y, boxSize, new Color(180, 220, 255));
        }
        drawSlotNumber(g2, "4", x, y, boxSize);
    }

    private void drawSlotBox(Graphics2D g2, int x, int y, int boxSize, Color borderColor) {
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(3f));
        g2.drawRoundRect(x - 2, y - 2, boxSize + 4, boxSize + 4, 12, 12);
        g2.setColor(new Color(70, 70, 70, 220));
        g2.fillRoundRect(x, y, boxSize, boxSize, 12, 12);
        g2.setColor(new Color(180, 180, 180));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, boxSize, boxSize, 12, 12);
    }

    private void drawSlotLabel(Graphics2D g2, String label, int x, int y, int boxSize, Color color) {
        g2.setFont(new Font("Arial", Font.PLAIN, 10));
        g2.setColor(color);
        int labelWidth = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, x + (boxSize - labelWidth) / 2, y - 6);
    }

    private void drawSlotNumber(Graphics2D g2, String num, int x, int y, int boxSize) {
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        g2.setColor(new Color(200, 200, 200, 180));
        g2.drawString(num, x + boxSize - 13, y + boxSize - 6);
    }

    // -------------------------------------------------------------------------

    private void drawMap(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        boolean[][] visible    = levelManager.getVisible();
        float[][]   lightLevel = levelManager.getLightLevel();
        List<Smoke> smokes     = levelManager.getSmokes();

        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                Tile tile  = levelManager.getMap().getTile(x, y);
                int drawX  = x * Constants.TILE_SIZE;
                int drawY  = y * Constants.TILE_SIZE;
                switch (tile) {
                    case WALL:
                        if (AssetLoader.get().wall != null)
                            g2.drawImage(AssetLoader.get().wall, drawX, drawY,
                                    Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                        else {
                            g2.setColor(new Color(90, 90, 90));
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        }
                        break;
                    case FLOOR:
                    case START:
                        if (AssetLoader.get().floor != null) {
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
                        if (AssetLoader.get().torch != null)
                            g2.drawImage(AssetLoader.get().torch, drawX, drawY,
                                    Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                        else {
                            g2.setColor(new Color(255, 140, 0));
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        }
                        break;
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
                        if (AssetLoader.get().floor != null)
                            g2.drawImage(AssetLoader.get().floor, drawX, drawY,
                                    Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                        else {
                            g2.setColor(new Color(170, 170, 170));
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
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
                    } else if (ground instanceof Shuriken) {
                        if (AssetLoader.get().shuriken != null) {
                            g2.drawImage(AssetLoader.get().shuriken, tx + iconPad, ty + iconPad,
                                    Constants.TILE_SIZE - 2 * iconPad, Constants.TILE_SIZE - 2 * iconPad, null);
                        } else {
                            g2.setColor(new Color(160, 200, 240, 200));
                            g2.fillOval(tx + iconPad, ty + iconPad,
                                    Constants.TILE_SIZE - 2 * iconPad, Constants.TILE_SIZE - 2 * iconPad);
                        }
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
            if (!visible[ty][tx]) continue;
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
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                if (!visible[y][x]) {
                    g2.setColor(new Color(0, 0, 0, 200));
                    g2.fillRect(x * Constants.TILE_SIZE, y * Constants.TILE_SIZE,
                            Constants.TILE_SIZE, Constants.TILE_SIZE);
                } else {
                    float light = lightLevel[y][x];
                    int alpha = (int) ((1.0f - light) * 200);
                    alpha = Math.max(0, Math.min(200, alpha));
                    if (alpha > 0) {
                        g2.setColor(new Color(0, 0, 0, alpha));
                        g2.fillRect(x * Constants.TILE_SIZE, y * Constants.TILE_SIZE,
                                Constants.TILE_SIZE, Constants.TILE_SIZE);
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

        if (visible[player.getY()][player.getX()]) {
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
            if (!visible[ey][ex]) continue;

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
                    if (badgeY < 2) badgeY = 2;

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
    }

    private void drawShurikenAimOverlay(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int ts = Constants.TILE_SIZE;
        Player player = levelManager.getPlayer();
        int px = player.getX();
        int py = player.getY();
        int dx = panel.getShurikenDx();
        int dy = panel.getShurikenDy();
        unseen.map.Map map = levelManager.getMap();

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRect(0, 0, panel.getWidth(), panel.getHeight());

        boolean hitEnemy = false;
        for (int i = 1; i <= 5; i++) {
            int tx = px + dx * i;
            int ty = py + dy * i;
            if (tx < 0 || tx >= Constants.GRID_WIDTH || ty < 0 || ty >= Constants.GRID_HEIGHT) break;
            if (map.getTile(tx, ty) == unseen.map.Tile.WALL) {
                g2.setColor(new Color(200, 50, 50, 120));
                g2.fillRect(tx * ts, ty * ts, ts, ts);
                break;
            }

            boolean enemyHere = false;
            for (unseen.entities.Enemy e : levelManager.getEnemies()) {
                if (e.isAlive() && e.getX() == tx && e.getY() == ty) {
                    enemyHere = true;
                    hitEnemy = true;
                    break;
                }
            }

            if (enemyHere) {
                g2.setColor(new Color(220, 60, 60, 200));
                g2.fillRect(tx * ts, ty * ts, ts, ts);
                g2.setColor(new Color(255, 100, 100, 255));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRect(tx * ts, ty * ts, ts, ts);
                break;
            } else {
                g2.setColor(new Color(150, 220, 255, 80));
                g2.fillRect(tx * ts, ty * ts, ts, ts);
                g2.setColor(new Color(180, 240, 255, 180));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(tx * ts, ty * ts, ts, ts);
            }
        }

        int startCx = px * ts + ts / 2;
        int startCy = py * ts + ts / 2;
        int endCx   = startCx + dx * 5 * ts;
        int endCy   = startCy + dy * 5 * ts;
        g2.setColor(hitEnemy ? new Color(255, 80, 80, 160) : new Color(180, 240, 255, 120));
        float[] dash = {4f, 4f};
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
        g2.drawLine(startCx, startCy, endCx, endCy);

        String msg = (hitEnemy ? "Will hit enemy!  " : "") + "WASD to aim  |  Space/Enter to throw  |  Esc to cancel";
        g2.setFont(new Font("Arial", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        int msgW = fm.stringWidth(msg);
        int bx = (panel.getWidth() - msgW) / 2 - 12;
        int by = panel.getHeight() - 42;
        g2.setColor(new Color(20, 20, 20, 190));
        g2.setStroke(new BasicStroke(1f));
        g2.fillRoundRect(bx, by, msgW + 24, 28, 10, 10);
        g2.setColor(hitEnemy ? new Color(255, 160, 160) : new Color(180, 240, 255));
        g2.drawString(msg, bx + 12, by + 20);
    }

    private void drawFlares(Graphics g) {
        List<ActiveFlare> flares = levelManager.getFlares();
        if (flares.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean[][] visible = levelManager.getVisible();
        long now = System.currentTimeMillis();
        int ts = Constants.TILE_SIZE;
        for (ActiveFlare f : flares) {
            if (!visible[f.getY()][f.getX()]) continue;
            int cx = f.getX() * ts + ts / 2;
            int cy = f.getY() * ts + ts / 2;

            float animCycle = (float) ((now % 1200) / 1200.0);
            int frame = (int) (animCycle * 8);

            int alpha = 40;
            if (frame >= 2 && frame <= 4) alpha = 60;
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
                star.moveTo(cx - r, cy - r); star.lineTo(cx + r, cy - r);
                star.lineTo(cx + r, cy + r); star.lineTo(cx - r, cy + r);
                star.closePath();
            } else {
                if      (frame == 1 || frame == 6) { r = ts * 0.2; innerCore = r * 0.15; }
                else if (frame == 2 || frame == 5) { r = ts * 0.4; innerCore = r * 0.15; }
                else if (frame == 3)               { r = ts * 0.6; innerCore = r * 0.15; }
                else if (frame == 4)               { r = ts * 0.4; innerCore = r * 0.15; }
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
                float partDist; int pSize;
                if      (frame == 4) { partDist = ts * 0.45f; pSize = 3; }
                else if (frame == 5) { partDist = ts * 0.65f; pSize = 2; }
                else                 { partDist = ts * 0.8f;  pSize = 1; }
                g2.fillOval(cx - (int) partDist - pSize, cy - (int) partDist - pSize, pSize * 2, pSize * 2);
                g2.fillOval(cx + (int) partDist - pSize, cy - (int) partDist - pSize, pSize * 2, pSize * 2);
                g2.fillOval(cx - (int) partDist - pSize, cy + (int) partDist - pSize, pSize * 2, pSize * 2);
                g2.fillOval(cx + (int) partDist - pSize, cy + (int) partDist - pSize, pSize * 2, pSize * 2);
            }
        }
    }

    private void drawNoiseFlashes(Graphics g) {
        List<FlashEffect> noiseFlashes = levelManager.getNoiseFlashes();
        if (noiseFlashes.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        java.awt.Stroke saved = g2.getStroke();
        long now = System.currentTimeMillis();
        int ts = Constants.TILE_SIZE;
        for (FlashEffect f : noiseFlashes) {
            int cx = f.getX() * ts + ts / 2;
            int cy = f.getY() * ts + ts / 2;
            int baseAlpha = f.getCountdown() * 55;
            float t1 = (float) ((now % 600) / 600.0);
            int r1 = ts / 4 + (int) (ts * 0.9f * t1);
            int a1 = Math.min(255, (int) (baseAlpha * (1.0f - t1 * 0.5f)));
            g2.setColor(new Color(255, 210, 50, a1));
            g2.setStroke(new BasicStroke(3f));
            g2.drawOval(cx - r1, cy - r1, r1 * 2, r1 * 2);
            float t2 = (float) (((now + 200) % 600) / 600.0);
            int r2 = ts / 4 + (int) (ts * 0.9f * t2);
            int a2 = Math.min(255, (int) (baseAlpha * (1.0f - t2 * 0.5f)));
            g2.setColor(new Color(255, 130, 20, a2));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - r2, cy - r2, r2 * 2, r2 * 2);
        }
        g2.setStroke(saved);
    }

    private void drawTargetingOverlay(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int ts = Constants.TILE_SIZE;

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(0, 0, panel.getWidth(), panel.getHeight());

        int mouseGridX = panel.getMouseGridX();
        int mouseGridY = panel.getMouseGridY();

        boolean validTile = mouseGridX >= 0 && mouseGridY >= 0
                && mouseGridX < Constants.GRID_WIDTH && mouseGridY < Constants.GRID_HEIGHT
                && levelManager.getMap().isPassable(mouseGridX, mouseGridY);

        int tx = mouseGridX * ts;
        int ty = mouseGridY * ts;

        if (panel.isTargetingShuriken())
            drawShurikenAimOverlay(g);

        if (validTile) {
            if (panel.isTargetingFlare()) {
                g2.setColor(new Color(255, 240, 100, 130)); g2.fillRect(tx, ty, ts, ts);
                g2.setColor(new Color(255, 255, 150, 220));
                g2.setStroke(new BasicStroke(2.5f)); g2.drawRect(tx, ty, ts, ts);
                int cx = tx + ts / 2, cy = ty + ts / 2;
                g2.setColor(new Color(255, 255, 150, 200));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(cx - ts / 2, cy, cx + ts / 2, cy);
                g2.drawLine(cx, cy - ts / 2, cx, cy + ts / 2);
            } else {
                g2.setColor(new Color(255, 200, 50, 130)); g2.fillRect(tx, ty, ts, ts);
                g2.setColor(new Color(255, 220, 80, 220));
                g2.setStroke(new BasicStroke(2.5f)); g2.drawRect(tx, ty, ts, ts);
                int cx = tx + ts / 2, cy = ty + ts / 2;
                g2.setColor(new Color(255, 220, 80, 200));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(cx - ts / 2, cy, cx + ts / 2, cy);
                g2.drawLine(cx, cy - ts / 2, cx, cy + ts / 2);
            }
        } else if (mouseGridX >= 0 && mouseGridY >= 0
                && mouseGridX < Constants.GRID_WIDTH && mouseGridY < Constants.GRID_HEIGHT) {
            g2.setColor(new Color(200, 50, 50, 100)); g2.fillRect(tx, ty, ts, ts);
            g2.setColor(new Color(200, 80, 80, 180));
            g2.setStroke(new BasicStroke(2f)); g2.drawRect(tx, ty, ts, ts);
        }

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

        Color validColor = panel.isTargetingFlare() ? new Color(255, 255, 120) : new Color(255, 220, 80);
        g2.setColor(validTile ? validColor : new Color(220, 100, 100));
        g2.drawString(msg, bx + 12, by + 20);
    }
}