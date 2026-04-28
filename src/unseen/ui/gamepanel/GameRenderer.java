package unseen.ui.gamepanel;

import unseen.entities.Enemy;
import unseen.entities.PatrolEnemy;
import unseen.entities.Player;
import unseen.entities.SentryEnemy;
import unseen.game.ActiveFlare;
import unseen.game.GameState;
import unseen.game.Smoke;
import unseen.items.Flare;
import unseen.items.GrapplingHook;
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

        if (panel.isJumpscareActive()) {
            drawJumpscare((Graphics2D) g);
            return;
        }

        // -- Screen shake --------------------------------------------------------
        unseen.ui.gamepanel.ScreenShake shake = panel.getScreenShake();
        shake.update();
        Graphics2D world = (Graphics2D) g.create();
        if (shake.isActive()) {
            world.translate((int) shake.getOffsetX(), (int) shake.getOffsetY());
        }
        // ------------------------------------------------------------------------

        drawMap(world);
        drawEntities(world);
        drawFlares(world);
        drawNoiseFlashes(world);
        drawShurikenProjectiles(world);
        drawGrappleAnimation(world);

        // --- HORROR ATMOSPHERE ---
        if (panel.isHorrorMode() && !levelManager.isFloorPurified()) {
            drawAtmosphericEffects(world);
        }

        // --- HORROR MODE: Heartbeat Vignette ---
        if (panel.isHorrorMode() && !levelManager.isFloorPurified()
                && panel.getGameState() == unseen.game.GameState.PLAYING) {
            double minEnemyDist = Double.MAX_VALUE;
            unseen.entities.Player p = levelManager.getPlayer();

            boolean anyChase = false;
            for (unseen.entities.Enemy e : levelManager.getEnemies()) {
                if (e.getState() == unseen.entities.Enemy.State.CHASE || e instanceof unseen.entities.StalkerEnemy) {
                    anyChase = true;
                    double d = Math.hypot(e.getX() - p.getX(), e.getY() - p.getY());
                    if (d < minEnemyDist)
                        minEnemyDist = d;
                }
            }

            long now = System.currentTimeMillis();
            boolean highTension = levelManager.isHighTension();

            float pulse = 0;
            if (anyChase) {
                // Faster pulse as they get closer
                double speed = 0.8 + Math.max(0, 10.0 - minEnemyDist) * 1.2;
                pulse = (float) (0.3 + 0.7 * 0.5 * (1.0 + Math.sin(now * 0.01 * speed)));
            } else if (highTension) {
                // Subtle static dark pulse in high tension
                pulse = (float) (0.1 + 0.1 * 0.5 * (1.0 + Math.sin(now * 0.005)));
            }

            int w = panel.getWidth();
            int h = panel.getHeight();
            Graphics2D g2 = (Graphics2D) g.create();

            // Red vignette only if chasing, otherwise black/dark red
            Color edgeColor = anyChase ? new Color(180, 0, 0, (int) (180 * pulse))
                    : new Color(0, 0, 0, (int) (150 * pulse));

            RadialGradientPaint vignette = new RadialGradientPaint(
                    w / 2f, h / 2f, Math.max(w, h) * 0.95f,
                    new float[] { 0.2f, 1.0f },
                    new Color[] { new Color(0, 0, 0, 0), edgeColor });
            g2.setPaint(vignette);
            g2.fillRect(0, 0, w, h);

            // --- PROXIMITY GLITCH --- Extremely rare now
            if (minEnemyDist < 4.0 || (highTension && minEnemyDist < 7.0 && Math.random() < 0.08)) {
                Random r = new Random();
                // Even fewer segments
                int segments = (highTension ? 2 : 1) + r.nextInt(2);
                for (int i = 0; i < segments; i++) {
                    int gy = r.nextInt(h);
                    int gh = 1 + r.nextInt(10);
                    int gox = r.nextInt(20) - 10;
                    g2.copyArea(0, gy, w, gh, gox, 0);

                    if (r.nextDouble() < 0.20) {
                        g2.setColor(new Color(255, 0, 0, 30));
                        g2.fillRect(0, gy, w, gh);
                    }
                }
            }
            g2.dispose();
        }

        // --- Shadow Figures ---
        if (panel.isHorrorMode() && !levelManager.isFloorPurified()) {
            for (unseen.entities.ShadowFigure sf : levelManager.getShadowFigures()) {
                int sdx = sf.getX() * Constants.TILE_SIZE;
                int sdy = sf.getY() * Constants.TILE_SIZE;
                world.setColor(new Color(0, 0, 0, 220));
                // Draw a simple silhouette
                world.fillOval(sdx + 10, sdy + 5, 12, 12); // head
                world.fillRect(sdx + 8, sdy + 15, 16, 15); // body
            }
        }
        // ----------------------

        // ----------------------------------------

        if (panel.isTargetingNoiseMaker() || panel.isTargetingFlare() || panel.isTargetingGrapplingHook())
            drawTargetingOverlay(world);
        if (panel.isTargetingShuriken())
            drawShurikenAimOverlay(world);

        world.dispose();

        // -- Red vignette flash on player hit
        // ------------------------------------------
        long hitAge = System.currentTimeMillis() - panel.getLastHitTime();
        if (hitAge < 600) { // 600ms red flash
            float fade = 1f - (hitAge / 600f);
            int alpha = (int) (120 * fade);
            Graphics2D vg = (Graphics2D) g.create();
            vg.setComposite(java.awt.AlphaComposite.getInstance(
                    java.awt.AlphaComposite.SRC_OVER, alpha / 255f));
            // Radial-ish vignette: dark-red edges, transparent center
            int w = panel.getWidth(), h = panel.getHeight();
            int border = (int) (Math.min(w, h) * 0.18f);
            vg.setColor(new Color(180, 20, 20));
            vg.fillRect(0, 0, w, border); // top
            vg.fillRect(0, h - border, w, border); // bottom
            vg.fillRect(0, border, border, h - 2 * border); // left
            vg.fillRect(w - border, border, border, h - 2 * border); // right
            vg.dispose();
        }

        // All overlay screens and HUD use unshifted `g` so they stay screen-locked
        if (panel.getGameState() == GameState.WIN) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, panel.getWidth(), panel.getHeight());

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int cx = panel.getWidth() / 2;
            int cy = panel.getHeight() / 2;

            String line1 = "Floor " + levelManager.getFloorNumber() + " Complete!";
            g.setColor(Color.GREEN);
            g.setFont(new Font("SansSerif", Font.BOLD, 42));
            int w1 = g.getFontMetrics().stringWidth(line1);
            g.drawString(line1, cx - w1 / 2, cy - 60);

            // Stats block
            unseen.game.RunStats stats = panel.getRunStats();
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.setColor(new Color(200, 200, 200));
            String s1 = "Turns: " + stats.getTurnsSurvived() + "   Kills: " + stats.getEnemiesKilled();
            int sw1 = g.getFontMetrics().stringWidth(s1);
            g.drawString(s1, cx - sw1 / 2, cy - 20);

            String line2 = "Press any key for Floor " + (levelManager.getFloorNumber() + 1);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 22));
            int w2 = g.getFontMetrics().stringWidth(line2);
            g.drawString(line2, cx - w2 / 2, cy + 16);

            String line3 = "ESC  --  Return to Menu";
            g.setColor(new Color(180, 180, 180));
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            int w3 = g.getFontMetrics().stringWidth(line3);
            g.drawString(line3, cx - w3 / 2, cy + 48);
        }

        if (panel.getGameState() == GameState.LOSE) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, panel.getWidth(), panel.getHeight());

            Graphics2D g2L = (Graphics2D) g;
            g2L.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int lcx = panel.getWidth() / 2;
            int lcy = panel.getHeight() / 2;

            String loseTitle = "GAME OVER";
            g.setColor(Color.RED);
            g.setFont(new Font("SansSerif", Font.BOLD, 48));
            int ltw = g.getFontMetrics().stringWidth(loseTitle);
            g.drawString(loseTitle, lcx - ltw / 2, lcy - 70);

            unseen.game.RunStats lstats = panel.getRunStats();
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.setColor(new Color(200, 200, 200));
            String ls1 = "Floor: " + levelManager.getFloorNumber()
                    + "   Turns: " + lstats.getTurnsSurvived()
                    + "   Kills: " + lstats.getEnemiesKilled();
            int lsw = g.getFontMetrics().stringWidth(ls1);
            g.drawString(ls1, lcx - lsw / 2, lcy - 28);

            if (lstats.isNewHighScore()) {
                g.setColor(new Color(255, 220, 50));
                g.setFont(new Font("SansSerif", Font.BOLD, 16));
                String hs = "\u2605 NEW HIGH SCORE! \u2605";
                int hsw = g.getFontMetrics().stringWidth(hs);
                g.drawString(hs, lcx - hsw / 2, lcy - 4);
            } else if (lstats.getHighScoreFloors() > 0) {
                g.setColor(new Color(140, 140, 150));
                g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                String hs = "Best: Floor " + lstats.getHighScoreFloors()
                        + " / " + lstats.getHighScoreTurns() + " turns";
                int hsw = g.getFontMetrics().stringWidth(hs);
                g.drawString(hs, lcx - hsw / 2, lcy - 4);
            }

            String loseHint = "R -- Restart     ESC -- Main Menu";
            g.setColor(new Color(200, 200, 200));
            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            int lhw = g.getFontMetrics().stringWidth(loseHint);
            g.drawString(loseHint, lcx - lhw / 2, lcy + 36);
        }

        if (panel.getGameState() == GameState.PAUSED) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, panel.getWidth(), panel.getHeight());

            String pauseText = "PAUSED";
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            int pw = g.getFontMetrics().stringWidth(pauseText);
            g.drawString(pauseText, (panel.getWidth() - pw) / 2, panel.getHeight() / 2 - 20);

            String resumeHint = "Any Key  --  Resume";
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            int rw = g.getFontMetrics().stringWidth(resumeHint);
            g.setColor(new Color(200, 200, 200));
            g.drawString(resumeHint, (panel.getWidth() - rw) / 2, panel.getHeight() / 2 + 25);

            String menuHint = "ESC  --  Return to Menu";
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            int mw = g.getFontMetrics().stringWidth(menuHint);
            g.setColor(new Color(160, 160, 160));
            g.drawString(menuHint, (panel.getWidth() - mw) / 2, panel.getHeight() / 2 + 55);
        }

        // -- Return-to-menu confirmation overlay ----------------------------------
        if (panel.getGameState() == GameState.CONFIRM_QUIT) {
            // Darken everything behind
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, panel.getWidth(), panel.getHeight());

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Card background
            int cardW = 380, cardH = 160;
            int cardX = (panel.getWidth() - cardW) / 2;
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
            String yes = "ESC  --  Yes, go to menu";
            String no = "Any other key  --  Stay";
            g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
            FontMetrics ofm = g2.getFontMetrics();

            int yw = ofm.stringWidth(yes);
            g2.setColor(new Color(180, 240, 140));
            g2.drawString(yes, cardX + (cardW - yw) / 2, cardY + 98);

            int nw = ofm.stringWidth(no);
            g2.setColor(new Color(180, 180, 180));
            g2.drawString(no, cardX + (cardW - nw) / 2, cardY + 126);
        }
        // ------------------------------------------------------------------------

        // Floor number in top-right corner
        if (panel.getGameState() == GameState.PLAYING) {
            String floorLabel = "Floor " + levelManager.getFloorNumber();
            g.setFont(new Font("Arial", Font.BOLD, 18));
            int lw = g.getFontMetrics().stringWidth(floorLabel);
            int rx = panel.getWidth() - lw - 14;
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRoundRect(rx - 6, 10, lw + 12, 26, 8, 8);
            g.setColor(new Color(255, 230, 120));
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

        // Always draw inventory bar + health + toasts
        drawInventory(g);
        if (panel.getGameState() == GameState.PLAYING) {
            drawHealthBar(g);
            drawToasts(g);
        }
        drawLoreNote(g);
    }

    /** Draws a simple heart shape (filled or outline). */
    private void drawHeart(Graphics2D g2, int x, int y, int size, boolean fill) {
        int[] xp = {
                x + size / 2,
                x + size,
                x + size,
                x + size * 3 / 4,
                x + size / 2,
                x + size / 4,
                x,
                x
        };
        int[] yp = {
                y + size * 3 / 8,
                y,
                y + size / 4,
                y + size / 2,
                y + size,
                y + size / 2,
                y + size / 4,
                y
        };
        if (fill) {
            g2.fillPolygon(xp, yp, 8);
        } else {
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawPolygon(xp, yp, 8);
        }
    }

    // -------------------------------------------------------------------------
    // Toast notifications -- drawn from bottom-center, stacking upward
    // -------------------------------------------------------------------------
    private void drawToasts(Graphics g) {
        java.util.List<HudToast> toasts = panel.getToasts();
        // Prune expired
        toasts.removeIf(HudToast::isExpired);
        if (toasts.isEmpty())
            return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int baseY = panel.getHeight() - 56;
        int cx = panel.getWidth() / 2;

        // Show up to 3 most recent toasts
        int start = Math.max(0, toasts.size() - 3);
        for (int i = start; i < toasts.size(); i++) {
            HudToast toast = toasts.get(i);
            float alpha = toast.getAlpha();
            if (alpha <= 0)
                continue;

            int slot = toasts.size() - 1 - i; // 0 = bottom (newest)
            int ty = baseY - slot * 26 - 60; // offset upward to avoid bottom health bar

            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            String msg = toast.getMessage();
            int tw = fm.stringWidth(msg);
            int pad = 12;

            // Pill background
            java.awt.Composite saved = g2.getComposite();
            g2.setComposite(java.awt.AlphaComposite.getInstance(
                    java.awt.AlphaComposite.SRC_OVER, alpha * 0.75f));
            g2.setColor(new Color(10, 10, 12));
            g2.fillRoundRect(cx - tw / 2 - pad, ty - fm.getAscent() - 4,
                    tw + pad * 2, fm.getHeight() + 8, 16, 16);

            // Border
            g2.setComposite(java.awt.AlphaComposite.getInstance(
                    java.awt.AlphaComposite.SRC_OVER, alpha * 0.5f));
            g2.setColor(new Color(
                    toast.getColor().getRed(),
                    toast.getColor().getGreen(),
                    toast.getColor().getBlue(), 140));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(cx - tw / 2 - pad, ty - fm.getAscent() - 4,
                    tw + pad * 2, fm.getHeight() + 8, 16, 16);

            // Text
            g2.setComposite(java.awt.AlphaComposite.getInstance(
                    java.awt.AlphaComposite.SRC_OVER, alpha));
            g2.setColor(toast.getColor());
            g2.drawString(msg, cx - tw / 2, ty);

            g2.setComposite(saved);
        }
    }

    private void drawHealthBar(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int hp = levelManager.getPlayer().getHealth();
        int maxHp = unseen.entities.Player.MAX_HEALTH;
        int heartSz = 32;
        int spacing = 6;

        int pillW = maxHp * (heartSz + spacing) + 16;
        int pillH = heartSz + 16;
        int bx = (panel.getWidth() - pillW) / 2;
        int by = panel.getHeight() - pillH - 12;

        // Pulsing effect when HP is low
        long now = System.currentTimeMillis();
        float pulse = (float) (0.5 + 0.5 * Math.sin(now / 250.0));

        // Background pill
        g.setColor(new Color(12, 12, 14, 210));
        g2.fillRoundRect(bx, by, pillW, pillH, 18, 18);
        g.setColor(new Color(80, 80, 90, 160));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(bx, by, pillW, pillH, 18, 18);

        Image heartImg = AssetLoader.get().heart;
        for (int i = 0; i < maxHp; i++) {
            int hx = bx + 10 + i * (heartSz + spacing);
            int hy = by + 8;
            float alpha = (i < hp) ? 1.0f : 0.25f;
            if (hp == 1 && i == 0)
                alpha = 0.6f + 0.4f * pulse;

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            if (heartImg != null) {
                g2.drawImage(heartImg, hx, hy, heartSz, heartSz, null);
            } else {
                g2.setColor(i < hp ? new Color(220, 50, 40) : new Color(60, 60, 65));
                g2.fillOval(hx, hy, heartSz, heartSz);
            }
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
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
                new float[] { 0.0f, 1.0f },
                new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 160) });
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
        g2.fillRect(cardX - 2, cardY - 2, cs, cs);
        g2.fillRect(cardX + cardW - cs + 2, cardY - 2, cs, cs);
        g2.fillRect(cardX - 2, cardY + cardH - cs + 2, cs, cs);
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
        int[] dpx = { dx, dx + ds, dx, dx - ds };
        int[] dpy = { dy - ds, dy, dy + ds, dy };
        g2.setColor(new Color(140, 90, 25, 220));
        g2.fillPolygon(dpx, dpy, 4);

        // -- Menu options -- all uniform plain text style --------------------------
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
        g2.drawString(howTo, (w - hw2) / 2 + 1, optY + 33);
        g2.setColor(new Color(118, 100, 72));
        g2.drawString(howTo, (w - hw2) / 2, optY + 32);

        String horror = "X : Horror Mode [" + (panel.isHorrorMode() ? "ON" : "OFF") + "]";
        g2.setFont(new Font("Serif", Font.BOLD, 19));
        int hwx = g2.getFontMetrics().stringWidth(horror);
        g2.setColor(new Color(0, 0, 0, 130));
        g2.drawString(horror, (w - hwx) / 2 + 1, optY + 68);
        if (panel.isHorrorMode()) {
            g2.setColor(new Color(220, 80, 20)); // Bright horror orange
        } else {
            g2.setColor(new Color(118, 100, 72));
        }
        g2.drawString(horror, (w - hwx) / 2, optY + 67);

        String quit = "Q : Quit";
        g2.setFont(new Font("Serif", Font.PLAIN, 18));
        int qw = g2.getFontMetrics().stringWidth(quit);
        g2.setColor(new Color(0, 0, 0, 130));
        g2.drawString(quit, (w - qw) / 2 + 1, optY + 101);
        g2.setColor(new Color(118, 100, 72));
        g2.drawString(quit, (w - qw) / 2, optY + 100);
        // -- end menu options -----------------------------------------------------

        String hint = "WASD - Move     E - Pick up     1/2/3/4/5 - Items";
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
                if (levelManager.getMap().getTile(tx, ty) != Tile.TORCH)
                    continue;

                float phase = (float) ((tx * 17 + ty * 23) * 0.21);
                float flicker = (float) (0.5 + 0.5 * Math.sin(now / 155.0 + phase));

                int cx = tx * Constants.TILE_SIZE + Constants.TILE_SIZE / 2;
                int cy = ty * Constants.TILE_SIZE + Constants.TILE_SIZE / 2;

                int outer = (int) (Constants.TILE_SIZE * (2.8 + 0.8 * flicker));
                int mid = (int) (Constants.TILE_SIZE * (1.6 + 0.5 * flicker));
                int core = (int) (Constants.TILE_SIZE * (0.7 + 0.2 * flicker));

                g2.setColor(new Color(160, 80, 10, 18 + (int) (14 * flicker)));
                g2.fillOval(cx - outer / 2, cy - outer / 2, outer, outer);

                g2.setColor(new Color(230, 130, 30, 28 + (int) (22 * flicker)));
                g2.fillOval(cx - mid / 2, cy - mid / 2, mid, mid);

                g2.setColor(new Color(255, 210, 100, 50 + (int) (30 * flicker)));
                g2.fillOval(cx - core / 2, cy - core / 2, core, core);
            }
        }
    }

    // =========================================================================
    // Inventory HUD -- slots 1-4 with keybind badges, item names, empty state
    // =========================================================================

    /** One descriptor per inventory slot (order = visual left->right). */
    private static final class SlotDef {
        final String key; // keyboard hint shown in badge
        final String itemName; // human-readable label shown above slot
        final Color borderFull; // border tint when item is present
        final Color borderEmpty; // border tint when slot is empty/used

        SlotDef(String key, String itemName, Color full, Color empty) {
            this.key = key;
            this.itemName = itemName;
            this.borderFull = full;
            this.borderEmpty = empty;
        }
    }

    private static final SlotDef[] SLOTS = {
            new SlotDef("1", "Noise Maker", new Color(255, 240, 130), new Color(110, 105, 70)),
            new SlotDef("2", "Smoke Bomb", new Color(180, 200, 255), new Color(80, 85, 110)),
            new SlotDef("3", "Lantern", new Color(255, 230, 120), new Color(110, 100, 60)),
            new SlotDef("4", "Shuriken", new Color(140, 210, 255), new Color(65, 95, 120)),
            new SlotDef("5", "Grapple", new Color(150, 220, 255), new Color(70, 95, 110)),
            new SlotDef("6", "Holy Cross", new Color(255, 255, 180), new Color(100, 100, 70)),
    };

    private void drawInventory(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        final int BOX = 48; // slot icon area
        final int SPACING = 10;
        final int LABEL_H = 14; // px above the box for the item name
        final int BADGE_H = 14; // px below the box for the keybind hint
        final int PAD_X = 16;
        final int PAD_Y = 8;

        int slots = panel.isHorrorMode() ? SLOTS.length : SLOTS.length - 1;
        int barW = PAD_X * 2 + slots * BOX + (slots - 1) * SPACING;
        int barH = PAD_Y * 2 + LABEL_H + BOX + BADGE_H;
        int barX = (panel.getWidth() - barW) / 2;
        int barY = 6;

        // -- Bar background ----------------------------------------------------
        g2.setColor(new Color(12, 12, 14, 210));
        g2.fillRoundRect(barX, barY, barW, barH, 20, 20);

        // Outer border
        g2.setColor(new Color(80, 80, 90, 160));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(barX, barY, barW, barH, 20, 20);

        // -- Inventory state ---------------------------------------------------
        Player player = levelManager.getPlayer();
        int countNoise = (int) player.getInventory().stream().filter(i -> i instanceof NoiseMaker).count();
        int countSmoke = (int) player.getInventory().stream().filter(i -> i instanceof SmokeBomb).count();
        int countFlare = (int) player.getInventory().stream().filter(i -> i instanceof Flare).count();
        int countShuriken = (int) player.getInventory().stream().filter(i -> i instanceof Shuriken).count();
        int countHook = (int) player.getInventory().stream().filter(i -> i instanceof GrapplingHook).count();
        int countCross = (int) player.getInventory().stream().filter(i -> i instanceof unseen.items.Cross).count();
        int[] counts = { countNoise, countSmoke, countFlare, countShuriken, countHook, countCross };

        java.awt.Image[] icons = {
                AssetLoader.get().noiseMaker,
                AssetLoader.get().smokeBomb,
                AssetLoader.get().lantern,
                AssetLoader.get().shuriken,
                AssetLoader.get().grapplingHook,
                AssetLoader.get().cross,
        };

        // Which slot is currently "active" (targeting)?
        boolean[] active = {
                panel.isTargetingNoiseMaker(),
                false, // smoke bomb has no targeting mode
                panel.isTargetingFlare(),
                panel.isTargetingShuriken(),
                panel.isTargetingGrapplingHook(),
                false, // cross is immediate
        };

        long now = System.currentTimeMillis();
        // Use the same timing for inventory pulse
        float invPulse = (float) (0.5 + 0.5 * Math.sin(now / 380.0)); // 0..1 glow wave

        int iconY = barY + PAD_Y + LABEL_H; // top of icon area

        for (int i = 0; i < slots; i++) {
            SlotDef def = SLOTS[i];
            int slotX = barX + PAD_X + i * (BOX + SPACING);
            int count = counts[i];
            boolean has = count > 0;
            boolean act = active[i];

            // -- Active glow halo behind the slot -----------------------------
            if (act) {
                int glowAlpha = 60 + (int) (80 * invPulse);
                g2.setColor(new Color(
                        def.borderFull.getRed(),
                        def.borderFull.getGreen(),
                        def.borderFull.getBlue(),
                        glowAlpha));
                int g2r = 8;
                g2.fillRoundRect(slotX - g2r, iconY - g2r, BOX + g2r * 2, BOX + g2r * 2, 18, 18);
            }

            // -- Slot body -----------------------------------------------------
            Color fill = has
                    ? new Color(50, 52, 58, 230)
                    : new Color(28, 28, 32, 180);
            g2.setColor(fill);
            g2.fillRoundRect(slotX, iconY, BOX, BOX, 10, 10);

            // Border: active -> pulsing gold/item-colour, full -> dim item-colour, empty ->
            // very dim
            Color border;
            if (act) {
                int ba = 160 + (int) (95 * invPulse);
                border = new Color(
                        Math.min(255, def.borderFull.getRed()),
                        Math.min(255, def.borderFull.getGreen()),
                        Math.min(255, def.borderFull.getBlue()),
                        ba);
                g2.setStroke(new BasicStroke(2.5f));
            } else if (has) {
                border = new Color(
                        def.borderFull.getRed(),
                        def.borderFull.getGreen(),
                        def.borderFull.getBlue(),
                        120);
                g2.setStroke(new BasicStroke(1.8f));
            } else {
                border = new Color(
                        def.borderEmpty.getRed(),
                        def.borderEmpty.getGreen(),
                        def.borderEmpty.getBlue(),
                        90);
                g2.setStroke(new BasicStroke(1.2f));
            }
            g2.setColor(border);
            g2.drawRoundRect(slotX, iconY, BOX, BOX, 10, 10);

            // -- Icon / empty-state --------------------------------------------
            int iconPad = 7;
            int iconSz = BOX - 2 * iconPad;
            if (has) {
                if (icons[i] != null) {
                    g2.drawImage(icons[i], slotX + iconPad, iconY + iconPad, iconSz, iconSz, null);
                } else {
                    // Geometric fallback
                    g2.setColor(def.borderFull);
                    g2.fillOval(slotX + iconPad, iconY + iconPad, iconSz, iconSz);
                }

                // -- Stack counter badge ----------------------------------------
                if (count > 1) {
                    String countStr = "x" + count;
                    g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                    FontMetrics cfm = g2.getFontMetrics();
                    int cw = cfm.stringWidth(countStr);

                    // Small dark background for the text
                    g2.setColor(new Color(0, 0, 0, 180));
                    g2.fillRoundRect(slotX + BOX - cw - 4, iconY + BOX - 15, cw + 2, 12, 4, 4);

                    g2.setColor(Color.WHITE);
                    g2.drawString(countStr, slotX + BOX - cw - 3, iconY + BOX - 5);
                }
            } else {
                // Empty: greyed X mark
                int xPad = iconPad + 4;
                g2.setColor(new Color(80, 80, 85, 120));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(slotX + xPad, iconY + xPad,
                        slotX + BOX - xPad, iconY + BOX - xPad);
                g2.drawLine(slotX + BOX - xPad, iconY + xPad,
                        slotX + xPad, iconY + BOX - xPad);
            }

            // -- Item name label (above slot) ----------------------------------
            String label = has ? def.itemName : "-- empty --";
            Color labelColor = has
                    ? new Color(def.borderFull.getRed(), def.borderFull.getGreen(),
                            def.borderFull.getBlue(), act ? 230 : 180)
                    : new Color(90, 90, 95, 160);
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            FontMetrics lfm = g2.getFontMetrics();
            int lw = lfm.stringWidth(label);
            g2.setColor(labelColor);
            g2.drawString(label, slotX + (BOX - lw) / 2, iconY - 3);

            // -- Keybind badge (below slot) ------------------------------------
            // Pill-shaped background + key character
            String key = def.key;
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            FontMetrics kfm = g2.getFontMetrics();
            int kw = kfm.stringWidth(key);
            int pillW = Math.max(kw + 10, 20);
            int pillH = 13;
            int pillX = slotX + (BOX - pillW) / 2;
            int pillY = iconY + BOX + 3;

            // Pill background
            Color pillBg = act
                    ? new Color(def.borderFull.getRed(), def.borderFull.getGreen(),
                            def.borderFull.getBlue(), 60 + (int) (50 * invPulse))
                    : new Color(40, 40, 45, 180);
            g2.setColor(pillBg);
            g2.fillRoundRect(pillX, pillY, pillW, pillH, pillH, pillH);

            // Pill border
            g2.setColor(act
                    ? new Color(def.borderFull.getRed(), def.borderFull.getGreen(),
                            def.borderFull.getBlue(), 160)
                    : new Color(90, 90, 100, 140));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(pillX, pillY, pillW, pillH, pillH, pillH);

            // Key character
            Color keyColor = act
                    ? new Color(def.borderFull.getRed(), def.borderFull.getGreen(),
                            def.borderFull.getBlue(), 230)
                    : (has ? new Color(200, 200, 210, 210) : new Color(100, 100, 108, 150));
            g2.setColor(keyColor);
            g2.drawString(key,
                    pillX + (pillW - kw) / 2,
                    pillY + kfm.getAscent() + (pillH - kfm.getHeight()) / 2);

            // -- "ACTIVE" micro-label when targeting ---------------------------
            if (act) {
                String actLabel = "ACTIVE";
                g2.setFont(new Font("SansSerif", Font.BOLD, 8));
                FontMetrics afm = g2.getFontMetrics();
                int aw = afm.stringWidth(actLabel);
                int actAlpha = 140 + (int) (115 * invPulse);
                g2.setColor(new Color(255, 220, 80, actAlpha));
                g2.drawString(actLabel, slotX + (BOX - aw) / 2, iconY + BOX + 3 + pillH + afm.getAscent() + 1);
            }
        }

        // Reset stroke
        g2.setStroke(new BasicStroke(1f));
    }

    // -------------------------------------------------------------------------

    private void drawMap(Graphics g) {
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

    private void drawEntities(Graphics g) {
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
            if (tx < 0 || tx >= Constants.GRID_WIDTH || ty < 0 || ty >= Constants.GRID_HEIGHT)
                break;
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
        int endCx = startCx + dx * 5 * ts;
        int endCy = startCy + dy * 5 * ts;
        g2.setColor(hitEnemy ? new Color(255, 80, 80, 160) : new Color(180, 240, 255, 120));
        float[] dash = { 4f, 4f };
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

    private void drawShurikenProjectiles(Graphics g) {
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

    private void drawGrappleAnimation(Graphics2D g2) {
        if (!panel.isGrappling()) return;

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
            int hx = (int)(sx + (wx - sx) * hookP);
            int hy = (int)(sy + (wy - sy) * hookP);
            
            // Draw rope (thick cable)
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(100, 100, 110));
            g2.drawLine(sx, sy, hx, hy);
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(180, 180, 200));
            g2.drawLine(sx, sy, hx, hy);
            
            // Draw hook sprite at the tip
            Image hookImg = AssetLoader.get().grappleNoRope;
            if (hookImg == null) hookImg = AssetLoader.get().grapplingHook; // Fallback
            
            if (hookImg != null) {
                int sz = (int)(ts * 0.7);
                double angle = Math.atan2(wy - sy, wx - sx);
                java.awt.geom.AffineTransform at = new java.awt.geom.AffineTransform();
                at.translate(hx, hy);
                at.rotate(angle + Math.PI/2); // Align top of sprite to direction
                at.translate(-sz/2.0, -sz/2.0);
                g2.drawImage(hookImg, at, null);
            }
        } else {
            // PHASE 2: Player zips
            float zipP = (progress - 0.4f) / 0.6f;
            int px = (int)(sx + (ex - sx) * zipP);
            int py = (int)(sy + (ey - sy) * zipP);
            
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
                int tx = (int)(sx + (ex - sx) * trailP);
                int ty = (int)(sy + (ey - sy) * trailP);
                g2.setColor(new Color(150, 220, 255, 100 / i));
                g2.fillOval(tx - ts/4, ty - ts/4, ts/2, ts/2);
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
                && (panel.isTargetingGrapplingHook()
                    ? levelManager.getMap().getTile(mouseGridX, mouseGridY) == Tile.WALL
                    : levelManager.getMap().isPassable(mouseGridX, mouseGridY));

        int tx = mouseGridX * ts;
        int ty = mouseGridY * ts;

        if (panel.isTargetingShuriken())
            drawShurikenAimOverlay(g);

        if (validTile) {
            if (panel.isTargetingGrapplingHook()) {
                g2.setColor(new Color(120, 200, 255, 130));
                g2.fillRect(tx, ty, ts, ts);
                g2.setColor(new Color(180, 230, 255, 220));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRect(tx, ty, ts, ts);
                int cx = tx + ts / 2, cy = ty + ts / 2;
                g2.drawOval(cx - ts / 4, cy - ts / 4, ts / 2, ts / 2);
                g2.drawLine(cx, cy, cx, cy + ts / 3);
            } else if (panel.isTargetingFlare()) {
                g2.setColor(new Color(255, 240, 100, 130));
                g2.fillRect(tx, ty, ts, ts);
                g2.setColor(new Color(255, 255, 150, 220));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRect(tx, ty, ts, ts);
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
                int cx = tx + ts / 2, cy = ty + ts / 2;
                g2.setColor(new Color(255, 220, 80, 200));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(cx - ts / 2, cy, cx + ts / 2, cy);
                g2.drawLine(cx, cy - ts / 2, cx, cy + ts / 2);
            }
        } else if (mouseGridX >= 0 && mouseGridY >= 0
                && mouseGridX < Constants.GRID_WIDTH && mouseGridY < Constants.GRID_HEIGHT) {
            g2.setColor(new Color(200, 50, 50, 100));
            g2.fillRect(tx, ty, ts, ts);
            g2.setColor(new Color(200, 80, 80, 180));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(tx, ty, ts, ts);
        }

        String msg;
        if (panel.isTargetingGrapplingHook()) {
            msg = validTile ? "WASD aim wall  |  SPACE hook  |  Esc cancel"
                    : "Need wall tile  |  Esc cancel";
        } else {
            String actionName = panel.isTargetingFlare() ? "flare" : "noise";
            msg = validTile ? "Click to throw " + actionName + "  |  Esc to cancel"
                    : "Invalid tile  |  Esc to cancel";
        }
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        int msgW = fm.stringWidth(msg);
        int bx = (panel.getWidth() - msgW) / 2 - 12;
        int by = panel.getHeight() - 42;
        g2.setColor(new Color(20, 20, 20, 190));
        g2.fillRoundRect(bx, by, msgW + 24, 28, 10, 10);

        Color validColor = panel.isTargetingGrapplingHook()
                ? new Color(180, 230, 255)
                : (panel.isTargetingFlare() ? new Color(255, 255, 120) : new Color(255, 220, 80));
        g2.setColor(validTile ? validColor : new Color(220, 100, 100));
        g2.drawString(msg, bx + 12, by + 20);
    }

    private void drawJumpscare(Graphics2D g2) {
        int w = panel.getWidth();
        int h = panel.getHeight();

        // Dark red background with static flicker
        g2.setColor(new Color(20, 0, 0, 230));
        g2.fillRect(0, 0, w, h);

        Random rand = new Random();
        // Static noise
        for (int i = 0; i < 100; i++) {
            g2.setColor(new Color(rand.nextInt(100), 0, 0, 50));
            g2.fillRect(rand.nextInt(w), rand.nextInt(h), rand.nextInt(200), 2);
        }

        // Draw a terrifying distorted face
        int centerX = w / 2;
        int centerY = h / 2;

        // The "Eyes" (distorted red voids)
        g2.setColor(new Color(255, 0, 0, 180 + rand.nextInt(75)));
        int eyeSize = 120 + rand.nextInt(20);
        int eyeDist = 140;

        // Left Eye
        g2.fillOval(centerX - eyeDist - eyeSize / 2 + rand.nextInt(10),
                centerY - 100 + rand.nextInt(10), eyeSize, eyeSize + rand.nextInt(50));
        // Right Eye
        g2.fillOval(centerX + eyeDist - eyeSize / 2 + rand.nextInt(10),
                centerY - 100 + rand.nextInt(10), eyeSize, eyeSize + rand.nextInt(50));

        // Pupils (pitch black)
        g2.setColor(Color.BLACK);
        g2.fillOval(centerX - eyeDist - 20, centerY - 80, 40, 40);
        g2.fillOval(centerX + eyeDist - 20, centerY - 80, 40, 40);

        // The "Mouth" (a screaming void)
        int mouthW = 200 + rand.nextInt(40);
        int mouthH = 300 + rand.nextInt(100);
        g2.setColor(new Color(0, 0, 0, 240));
        g2.fillOval(centerX - mouthW / 2 + rand.nextInt(5),
                centerY + 50 + rand.nextInt(10), mouthW, mouthH);

        // Sudden white flash pulses
        if (rand.nextBoolean()) {
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillRect(0, 0, w, h);
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
        }
    }

    private void drawLoreNote(Graphics g) {
        String lore = panel.getCurrentNoteLore();
        if (lore == null)
            return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = panel.getWidth();
        int h = panel.getHeight();

        g2.setFont(new Font("Serif", Font.ITALIC, 19));
        FontMetrics fm = g2.getFontMetrics();

        // Wrap text logic
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = lore.split(" ");
        StringBuilder sb = new StringBuilder();
        int maxW = 380; // slightly wider
        for (String word : words) {
            if (fm.stringWidth(sb.toString() + word) < maxW) {
                sb.append(word).append(" ");
            } else {
                lines.add(sb.toString());
                sb = new StringBuilder(word).append(" ");
            }
        }
        lines.add(sb.toString());

        int padding = 30;
        int boxW = maxW + padding * 2;
        int boxH = lines.size() * fm.getHeight() + padding * 2 + 10;
        int bx = (w - boxW) / 2;
        int by = (h - boxH) / 2 - 40;

        // 1. Draw "Torn Edge" background layers
        Random paperRand = new Random(lore.hashCode());
        g2.setColor(new Color(210, 200, 170, 240)); // Darker base for depth
        g2.fillRoundRect(bx + 4, by + 4, boxW, boxH, 4, 4);

        // Main parchment color
        g2.setColor(new Color(245, 240, 220));
        g2.fillRoundRect(bx, by, boxW, boxH, 2, 2);

        // Subtle "grain" and "stains"
        g2.setColor(new Color(230, 220, 190, 80));
        for (int i = 0; i < 5; i++) {
            int sx = bx + paperRand.nextInt(boxW - 40);
            int sy = by + paperRand.nextInt(boxH - 40);
            g2.fillOval(sx, sy, 30 + paperRand.nextInt(40), 20 + paperRand.nextInt(30));
        }

        // 2. Decorative borders (inner sketch lines)
        g2.setColor(new Color(110, 90, 70, 150));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(bx + 6, by + 6, boxW - 12, boxH - 12, 2, 2);

        // 3. Draw text in "Ink"
        g2.setColor(new Color(40, 35, 30)); // Dark sepia/ink
        int textY = by + padding + fm.getAscent();
        for (String line : lines) {
            int lw = fm.stringWidth(line.trim());
            g2.drawString(line.trim(), bx + (boxW - lw) / 2, textY);

            // Subtle "line" under the text like a notebook
            g2.setColor(new Color(100, 90, 80, 40));
            g2.drawLine(bx + padding, textY + 2, bx + boxW - padding, textY + 2);
            g2.setColor(new Color(40, 35, 30));

            textY += fm.getHeight();
        }

        // 4. Bloody Drips (Horror Mode only, 40% chance per note)
        if (panel.isHorrorMode() && paperRand.nextFloat() < 0.7f) {
            java.awt.Shape oldClip = g2.getClip();
            g2.clipRect(bx, by, boxW, boxH);

            // Realistic Blood Colors
            Color bloodBase = new Color(110, 0, 0, 220); // Thick fresh blood
            Color bloodDried = new Color(70, 5, 5, 180); // Dried clotted blood
            Color bloodThin = new Color(130, 10, 10, 130); // Translucent edge

            // Corner / Edge Splatters
            int splatterCount = 2 + paperRand.nextInt(3);
            for (int i = 0; i < splatterCount; i++) {
                int side = paperRand.nextInt(4); // 0:Top, 1:Right, 2:Bottom, 3:Left
                int sx, sy;
                if (side == 0) {
                    sx = bx + paperRand.nextInt(boxW);
                    sy = by;
                } else if (side == 1) {
                    sx = bx + boxW;
                    sy = by + paperRand.nextInt(boxH);
                } else if (side == 2) {
                    sx = bx + paperRand.nextInt(boxW);
                    sy = by + boxH;
                } else {
                    sx = bx;
                    sy = by + paperRand.nextInt(boxH);
                }

                // Main Splatter
                int size = 25 + paperRand.nextInt(35);
                drawRealisticSplatter(g2, sx, sy, size, paperRand, bloodBase, bloodDried, bloodThin);

                // Satellite droplets (using all three color variations)
                for (int s = 0; s < 5; s++) {
                    int ssx = sx + (paperRand.nextInt(size * 2) - size);
                    int ssy = sy + (paperRand.nextInt(size * 2) - size);
                    int sSize = 2 + paperRand.nextInt(6);

                    float colorRoll = paperRand.nextFloat();
                    if (colorRoll < 0.33f)
                        g2.setColor(bloodBase);
                    else if (colorRoll < 0.66f)
                        g2.setColor(bloodDried);
                    else
                        g2.setColor(bloodThin);

                    g2.fillOval(ssx, ssy, sSize, sSize);
                }
            }

            // Bloody Smears (like a hand dragged across)
            if (paperRand.nextFloat() < 0.4f) {
                int smearX = bx + 50 + paperRand.nextInt(boxW - 150);
                int smearY = by + 50 + paperRand.nextInt(boxH - 100);
                int smearLen = 80 + paperRand.nextInt(120);
                g2.setStroke(new BasicStroke(15f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int k = 0; k < 5; k++) {
                    int off = k * 3;
                    g2.setColor(new Color(90, 0, 0, 40 - k * 5));
                    g2.drawLine(smearX + off, smearY, smearX + off + smearLen / 2, smearY + smearLen);
                }
            }

            // High-Viscosity Drips
            int drips = 1 + paperRand.nextInt(2);
            for (int i = 0; i < drips; i++) {
                boolean leftSide = paperRand.nextBoolean();
                int dx = leftSide ? bx + 20 + paperRand.nextInt(80) : bx + boxW - 100 + paperRand.nextInt(80);
                int startY = by + paperRand.nextInt(20);
                int totalLen = 80 + paperRand.nextInt(150);

                for (int j = 0; j < totalLen; j += 3) {
                    float t = (float) j / totalLen;
                    // Drip gets thinner as it falls, then pools slightly at the end
                    int curW = (int) (6 * (1.0 - t * 0.7) + (t > 0.9 ? 3 : 0) + paperRand.nextInt(2));
                    int curX = dx + (int) (Math.sin(j * 0.05) * 2);

                    // Color darkens and gets more opaque at the bottom (pooling)
                    g2.setColor(new Color(
                            (int) (100 - t * 40),
                            0, 0,
                            (int) (180 + t * 75)));
                    g2.fillOval(curX - curW / 2, startY + j, curW, curW + 3);

                    // Occasional "bead" in the drip
                    if (paperRand.nextFloat() < 0.05f) {
                        g2.fillOval(curX - curW, startY + j, curW * 2, curW * 2);
                    }
                }
            }
            g2.setClip(oldClip);
        }
    }

    private void drawRealisticSplatter(Graphics2D g2, int x, int y, int size, Random rand, Color base, Color dried,
            Color thin) {
        // Use multiple overlapping ovals with slightly different colors and sizes to
        // create an organic shape
        for (int i = 0; i < 4; i++) {
            int ox = rand.nextInt(size / 2) - size / 4;
            int oy = rand.nextInt(size / 2) - size / 4;
            int curSize = size - rand.nextInt(size / 2);

            // Outer semi-transparent ring (using the specific thin color)
            g2.setColor(thin);
            g2.fillOval(x + ox - 3, y + oy - 3, curSize + 6, curSize + 6);

            // Thick center
            g2.setColor(rand.nextBoolean() ? base : dried);
            g2.fillOval(x + ox, y + oy, curSize, curSize);
        }
    }

    /**
     * Adds 'Found Footage' style grain, scanlines, and cold tint to the world.
     */
    private void drawAtmosphericEffects(Graphics2D g2) {
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
