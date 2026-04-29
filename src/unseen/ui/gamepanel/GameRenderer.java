package unseen.ui.gamepanel;

import unseen.game.GameState;
import unseen.ui.GamePanel;
import unseen.utils.Constants;

import java.awt.*;
import java.util.Random;

/**
 * Handles all rendering for the game panel, including the map, entities,
 * effects, HUD, and overlay screens.
 */
public class GameRenderer {

    public enum MenuAction {
        NONE, START, TUTORIAL,ACHIEVEMENTS, TOGGLE_HORROR, QUIT
    }

    private final GamePanel panel;
    private final LevelManager levelManager;
    private final WorldRenderer worldRenderer;
    private final MenuRenderer menuRenderer;
    private final HudRenderer hudRenderer;
    private final OverlayRenderer overlayRenderer;

    public GameRenderer(GamePanel panel, LevelManager levelManager) {
        this.panel = panel;
        this.levelManager = levelManager;
        this.worldRenderer = new WorldRenderer(panel, levelManager);
        this.menuRenderer = new MenuRenderer(panel, levelManager, worldRenderer);
        this.hudRenderer = new HudRenderer(panel, levelManager);
        this.overlayRenderer = new OverlayRenderer(panel, levelManager, worldRenderer, menuRenderer);
    }

    public MenuAction getMenuActionAt(int mouseX, int mouseY, int panelW, int panelH) {
        return menuRenderer.getMenuActionAt(mouseX, mouseY, panelW, panelH);
    }
    /** Entry point called from {@link GamePanel#paintComponent}. */
    public void render(Graphics g) {
        if (panel.getGameState() == GameState.INTRO) {
            overlayRenderer.drawIntroScreen(g);
            return;
        }

        if (panel.getGameState() == GameState.MENU) {
            menuRenderer.drawMainMenu(g);
            if (panel.getTutorial().isActive()) {
                panel.getTutorial().draw(g, panel.getWidth(), panel.getHeight());
            }
            return;
        }

        if (panel.isJumpscareActive()) {
            overlayRenderer.drawJumpscare((Graphics2D) g);
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

        worldRenderer.drawMap(world);
        worldRenderer.drawEntities(world);
        worldRenderer.drawFlares(world);
        worldRenderer.drawNoiseFlashes(world);
        worldRenderer.drawTileEffects(world);
        worldRenderer.drawShurikenProjectiles(world);
        worldRenderer.drawGrappleAnimation(world);

        // --- HORROR ATMOSPHERE ---
        if (panel.isHorrorMode() && !levelManager.isFloorPurified()) {
            worldRenderer.drawAtmosphericEffects(world);
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
            overlayRenderer.drawTargetingOverlay(world);
        if (panel.isTargetingShuriken())
            overlayRenderer.drawShurikenAimOverlay(world);

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

        if (panel.getGameState() == GameState.REWARD_CHOICE) {
            drawRewardChoiceOverlay(g);
        }

        drawQuestNotification(g);


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
            drawRoundQuestHud(g);
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
        hudRenderer.drawInventory(g);
        if (panel.getGameState() == GameState.PLAYING) {
            hudRenderer.drawHealthBar(g);
            hudRenderer.drawToasts(g);
        }
        hudRenderer.drawLoreNote(g);
    }

    private void drawRoundQuestHud(Graphics g) {
        unseen.game.QuestManager quests = panel.getQuestManager();
        if (quests == null || panel.getGameState() != GameState.PLAYING) {
            return;
        }

        if (!panel.isRoundQuestHudVisible()) {
            drawQuestHiddenHint(g);
            return;
        }

        unseen.game.QuestManager.Quest active = quests.getActiveQuest();
        if (active == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int x = 12;
        int y = 112;
        int w = 285;
        int h = 92;

        g2.setColor(new Color(0, 0, 0, 145));
        g2.fillRoundRect(x + 3, y + 4, w, h, 14, 14);
        g2.setPaint(new GradientPaint(x, y, new Color(36, 28, 18, 220),
                x, y + h, new Color(12, 10, 8, 232)));
        g2.fillRoundRect(x, y, w, h, 14, 14);
        g2.setColor(new Color(210, 162, 68, 190));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 14, 14);

        g2.setFont(new Font("Serif", Font.BOLD, 17));
        g2.setColor(new Color(255, 224, 120));
        g2.drawString("Round Quest", x + 12, y + 22);

        String progress = active.getProgress() + "/" + active.getTarget();
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        int progressW = g2.getFontMetrics().stringWidth(progress);
        g2.setColor(new Color(235, 205, 130));
        g2.drawString(progress, x + w - progressW - 12, y + 22);

        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.setColor(new Color(235, 220, 180));
        g2.drawString(fitText(g2, active.getName(), w - 24), x + 12, y + 43);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(new Color(185, 170, 135));
        g2.drawString(fitText(g2, active.getDescription(), w - 24), x + 12, y + 60);

        g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
        g2.setColor(new Color(150, 205, 150));
        g2.drawString(fitText(g2, "Hint: " + questHint(active), w - 24), x + 12, y + 76);

        int barX = x + 12;
        int barY = y + 83;
        int barW = w - 24;
        int barH = 5;
        float pct = active.getTarget() <= 0 ? 0f : active.getProgress() / (float) active.getTarget();
        pct = Math.max(0f, Math.min(1f, pct));

        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRoundRect(barX, barY, barW, barH, 5, 5);
        g2.setColor(new Color(255, 196, 75, 220));
        g2.fillRoundRect(barX, barY, Math.round(barW * pct), barH, 5, 5);

        g2.dispose();
    }

    private String questHint(unseen.game.QuestManager.Quest quest) {
        switch (quest.getEvent()) {
            case KILL:
                return "Defeat enemies with shuriken or smart positioning.";
            case PICKUP:
                return "Stand on an item and press E.";
            case TURN:
                return "Move, wait, or use items to spend turns.";
            case FLOOR_CLEAR:
                return "Find and step onto the real ladder.";
            default:
                return "Complete the listed objective.";
        }
    }

    private String fitText(Graphics2D g2, String text, int maxWidth) {
        if (text == null) {
            return "";
        }

        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }

        String clipped = text;
        while (clipped.length() > 3 && fm.stringWidth(clipped + "...") > maxWidth) {
            clipped = clipped.substring(0, clipped.length() - 1);
        }

        return clipped + "...";
    }




    private void drawQuestNotification(Graphics g) {
        String text = panel.getQuestNotificationText();
        if (text == null || panel.getGameState() != GameState.PLAYING) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setFont(new Font("Serif", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        int boxW = Math.min(panel.getWidth() - 80, fm.stringWidth(text) + 42);
        int x = (panel.getWidth() - boxW) / 2;
        int y = panel.getHeight() - 118;

        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(x + 4, y + 5, boxW, 42, 18, 18);
        g2.setPaint(new GradientPaint(x, y, new Color(80, 50, 12, 238),
                x, y + 42, new Color(28, 18, 8, 242)));
        g2.fillRoundRect(x, y, boxW, 42, 18, 18);
        g2.setColor(new Color(255, 220, 100, 220));
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(x, y, boxW, 42, 18, 18);
        g2.setColor(new Color(255, 238, 160));
        g2.drawString(text, x + (boxW - fm.stringWidth(text)) / 2, y + 28);
        g2.dispose();
    }



    private void drawRewardChoiceOverlay(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = panel.getWidth();
        int h = panel.getHeight();
        int cx = w / 2;
        int cy = h / 2;

        g2.setColor(new Color(0, 0, 0, 185));
        g2.fillRect(0, 0, w, h);

        String title = "Choose Your Floor Reward";
        g2.setFont(new Font("Serif", Font.BOLD, 38));
        FontMetrics titleFm = g2.getFontMetrics();
        g2.setColor(new Color(255, 224, 120));
        g2.drawString(title, cx - titleFm.stringWidth(title) / 2, cy - 96);

        String subtitle = "Pick 1 of 3 relics before descending.";
        g2.setFont(new Font("SansSerif", Font.PLAIN, 17));
        FontMetrics subFm = g2.getFontMetrics();
        g2.setColor(new Color(210, 195, 160));
        g2.drawString(subtitle, cx - subFm.stringWidth(subtitle) / 2, cy - 62);

        java.util.List<unseen.game.RewardChoice> choices = panel.getFloorRewardChoices();
        int cardW = 180;
        int cardH = 92;
        int gap = 18;
        int totalW = cardW * 3 + gap * 2;
        int startX = (w - totalW) / 2;
        int cardY = cy + 34;

        for (int i = 0; i < choices.size(); i++) {
            unseen.game.RewardChoice choice = choices.get(i);
            int x = startX + i * (cardW + gap);

            g2.setColor(new Color(0, 0, 0, 100));
            g2.fillRoundRect(x + 4, cardY + 5, cardW, cardH, 18, 18);
            g2.setPaint(new GradientPaint(x, cardY, new Color(42, 32, 22, 236),
                    x, cardY + cardH, new Color(16, 13, 10, 242)));
            g2.fillRoundRect(x, cardY, cardW, cardH, 18, 18);
            g2.setColor(new Color(215, 166, 72, 210));
            g2.setStroke(new BasicStroke(1.7f));
            g2.drawRoundRect(x, cardY, cardW, cardH, 18, 18);

            String key = "[" + (i + 1) + "]";
            g2.setFont(new Font("DialogInput", Font.BOLD, 14));
            g2.setColor(new Color(255, 220, 110));
            g2.drawString(key, x + 14, cardY + 25);

            g2.setFont(new Font("Serif", Font.BOLD, 20));
            FontMetrics nameFm = g2.getFontMetrics();
            String name = choice.getName();
            int nameX = x + (cardW - nameFm.stringWidth(name)) / 2;
            g2.setColor(new Color(238, 220, 180));
            g2.drawString(name, nameX, cardY + 58);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            String hint = "Click or press " + (i + 1);
            FontMetrics hintFm = g2.getFontMetrics();
            g2.setColor(new Color(150, 135, 100));
            g2.drawString(hint, x + (cardW - hintFm.stringWidth(hint)) / 2, cardY + 78);
        }

        String esc = "ESC  --  Return to Menu";
        g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
        FontMetrics escFm = g2.getFontMetrics();
        g2.setColor(new Color(170, 170, 170));
        g2.drawString(esc, cx - escFm.stringWidth(esc) / 2, cardY + cardH + 38);

        g2.dispose();
    }

    private void drawQuestHiddenHint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String text = "V - Quest";
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();

        int x = 12;
        int y = 112;
        int w = fm.stringWidth(text) + 20;
        int h = 24;

        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRoundRect(x, y, w, h, 10, 10);
        g2.setColor(new Color(210, 170, 80, 180));
        g2.drawRoundRect(x, y, w, h, 10, 10);
        g2.setColor(new Color(235, 215, 160));
        g2.drawString(text, x + 10, y + 17);

        g2.dispose();
    }



}
