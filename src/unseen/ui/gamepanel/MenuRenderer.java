package unseen.ui.gamepanel;

import unseen.map.Tile;
import unseen.ui.GamePanel;
import unseen.utils.Constants;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

class MenuRenderer {
    private final GamePanel panel;
    private final LevelManager levelManager;
    private final WorldRenderer worldRenderer;

    MenuRenderer(GamePanel panel, LevelManager levelManager, WorldRenderer worldRenderer) {
        this.panel = panel;
        this.levelManager = levelManager;
        this.worldRenderer = worldRenderer;
    }

private static final class MenuLayout {
        final int cardX;
        final int cardY;
        final int cardW;
        final int cardH;
        final int titleY;
        final int separatorY;
        final Rectangle startButton;
        final Rectangle tutorialButton;
        final Rectangle horrorButton;
        final Rectangle quitButton;

        MenuLayout(int cardX, int cardY, int cardW, int cardH, int titleY, int separatorY,
                Rectangle startButton, Rectangle tutorialButton, Rectangle horrorButton, Rectangle quitButton) {
            this.cardX = cardX;
            this.cardY = cardY;
            this.cardW = cardW;
            this.cardH = cardH;
            this.titleY = titleY;
            this.separatorY = separatorY;
            this.startButton = startButton;
            this.tutorialButton = tutorialButton;
            this.horrorButton = horrorButton;
            this.quitButton = quitButton;
        }
    }


    void drawMainMenu(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = panel.getWidth();
        int h = panel.getHeight();
        long now = System.currentTimeMillis();
        MenuLayout layout = buildMenuLayout(w, h);
        GameRenderer.MenuAction hovered = getMenuActionAt(panel.getMouseX(), panel.getMouseY(), w, h);

        Graphics2D bg = (Graphics2D) g2.create();
        double panX = Math.sin(now / 3300.0) * 12.0 + Math.cos(now / 1700.0) * 4.5;
        double panY = Math.cos(now / 4100.0) * 8.0 + Math.sin(now / 2100.0) * 3.0;
        double zoom = 1.06 + 0.022 * Math.sin(now / 4700.0) + 0.012 * Math.cos(now / 2600.0);
        bg.translate(panX - (w * (zoom - 1.0)) / 2.0, panY - (h * (zoom - 1.0)) / 2.0);
        bg.scale(zoom, zoom);
        worldRenderer.drawMap(bg);
        drawMenuTorchFlicker(bg, now);
        bg.dispose();

        drawMenuMist(g2, w, h, now);

        g2.setColor(new Color(0, 0, 0, 168));
        g2.fillRect(0, 0, w, h);

        java.awt.RadialGradientPaint vignette = new java.awt.RadialGradientPaint(
                w / 2f, h / 2f, Math.max(w, h) * 0.82f,
                new float[] { 0.10f, 0.58f, 1.0f },
                new Color[] {
                        new Color(0, 0, 0, 0),
                        new Color(8, 7, 5, 120),
                        new Color(0, 0, 0, 245)
                });
        g2.setPaint(vignette);
        g2.fillRect(0, 0, w, h);

        RoundRectangle2D.Float card = new RoundRectangle2D.Float(
                layout.cardX, layout.cardY, layout.cardW, layout.cardH, 38, 38);
        g2.setPaint(new GradientPaint(
                layout.cardX, layout.cardY, new Color(28, 21, 17, 196),
                layout.cardX, layout.cardY + layout.cardH, new Color(10, 8, 7, 232)));
        g2.fill(card);
        g2.setColor(new Color(255, 245, 220, 18));
        g2.fillRoundRect(layout.cardX + 16, layout.cardY + 16, layout.cardW - 32, 56, 30, 30);
        g2.setColor(new Color(214, 164, 70, 175));
        g2.setStroke(new BasicStroke(2.2f));
        g2.draw(card);
        g2.setColor(new Color(104, 72, 28, 160));
        g2.setStroke(new BasicStroke(1.1f));
        g2.drawRoundRect(layout.cardX + 7, layout.cardY + 7, layout.cardW - 14, layout.cardH - 14, 30, 30);

        float beatA = Math.max(0f, (float) Math.sin(now / 220.0));
        float beatB = Math.max(0f, (float) Math.sin((now - 150.0) / 220.0));
        float pulse = Math.min(1f, beatA * 0.9f + beatB * 0.55f);
        String title = "UNSEEN";
        Font titleFont = new Font("Serif", Font.BOLD, 68);
        g2.setFont(titleFont);
        int tw = g2.getFontMetrics().stringWidth(title);
        int titleX = (w - tw) / 2;
        int titleY = layout.titleY;

        g2.setColor(new Color(0, 0, 0, 220));
        g2.drawString(title, titleX + 6, titleY + 6);
        g2.setColor(new Color(35, 10, 0, 170));
        g2.drawString(title, titleX + 2, titleY + 2);

        int glowAlpha = 48 + (int) (96 * pulse);
        for (int r = 10; r >= 2; r--) {
            g2.setColor(new Color(255, 140 + r * 4, 30, Math.max(18, glowAlpha - r * 8)));
            g2.drawString(title, titleX - r, titleY);
            g2.drawString(title, titleX + r, titleY);
            g2.drawString(title, titleX, titleY - r);
            g2.drawString(title, titleX, titleY + r);
        }

        g2.setPaint(new GradientPaint(
                titleX, titleY - 60, new Color(255, 233, 151),
                titleX, titleY + 10, new Color(245, 144, 42)));
        g2.drawString(title, titleX, titleY);

        g2.setFont(new Font("Serif", Font.ITALIC, 18));
        String subtitle = "Every floor is listening.";
        int subtitleW = g2.getFontMetrics().stringWidth(subtitle);
        g2.setColor(new Color(185, 150, 95, 220));
        g2.drawString(subtitle, (w - subtitleW) / 2, titleY + 34);

        int sepY = layout.separatorY;
        int sepInset = 72;
        g2.setColor(new Color(120, 79, 26, 200));
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawLine(layout.cardX + sepInset, sepY, layout.cardX + layout.cardW - sepInset, sepY);
        g2.setColor(new Color(240, 199, 94, 135));
        g2.drawLine(layout.cardX + sepInset, sepY - 2, layout.cardX + layout.cardW - sepInset, sepY - 2);
        int dx = w / 2, dy = sepY;
        int ds = 7;
        int[] dpx = { dx, dx + ds, dx, dx - ds };
        int[] dpy = { dy - ds, dy, dy + ds, dy };
        g2.setColor(new Color(211, 165, 61, 235));
        g2.fillPolygon(dpx, dpy, 4);

        drawMenuPill(g2, layout.startButton, "SPACE", "Start Adventure",
                new Color(94, 56, 15, hovered == GameRenderer.MenuAction.START ? 235 : 208),
                new Color(255, 215, 120), hovered == GameRenderer.MenuAction.START, pulse);
        drawMenuPill(g2, layout.tutorialButton, "H", "How to Play",
                new Color(56, 43, 28, hovered == GameRenderer.MenuAction.TUTORIAL ? 228 : 202),
                new Color(220, 191, 126), hovered == GameRenderer.MenuAction.TUTORIAL, 0f);

        String horrorLabel = panel.isHorrorMode() ? "HORROR MODE (ON)" : "HORROR MODE (OFF)";
        Color horrorFill = panel.isHorrorMode()
                ? new Color(96, 30, 8, hovered == GameRenderer.MenuAction.TOGGLE_HORROR ? 242 : 216)
                : new Color(52, 37, 29, hovered == GameRenderer.MenuAction.TOGGLE_HORROR ? 226 : 198);
        Color horrorText = panel.isHorrorMode() ? new Color(255, 132, 42) : new Color(186, 152, 110);
        drawMenuPill(g2, layout.horrorButton, "X", horrorLabel, horrorFill, horrorText,
                hovered == GameRenderer.MenuAction.TOGGLE_HORROR, pulse);

        drawMenuPill(g2, layout.quitButton, "Q", "Leave the Dungeon",
                new Color(42, 34, 32, hovered == GameRenderer.MenuAction.QUIT ? 224 : 192),
                new Color(180, 162, 148), hovered == GameRenderer.MenuAction.QUIT, 0f);

        String hint = "WASD - Move     E - Pick up     1/2/3/4/5/6 - Items";
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


public GameRenderer.MenuAction getMenuActionAt(int mouseX, int mouseY, int panelW, int panelH) {
        MenuLayout layout = buildMenuLayout(panelW, panelH);
        Point point = new Point(mouseX, mouseY);
        if (layout.startButton.contains(point))
            return GameRenderer.MenuAction.START;
        if (layout.tutorialButton.contains(point))
            return GameRenderer.MenuAction.TUTORIAL;
        if (layout.horrorButton.contains(point))
            return GameRenderer.MenuAction.TOGGLE_HORROR;
        if (layout.quitButton.contains(point))
            return GameRenderer.MenuAction.QUIT;
        return GameRenderer.MenuAction.NONE;
    }


private MenuLayout buildMenuLayout(int w, int h) {
        int cardW = Math.min(560, w - 120);
        int cardH = Math.min(500, h - 150);
        int cardX = (w - cardW) / 2;
        int cardY = (h - cardH) / 2;
        int titleY = cardY + 120;
        int separatorY = titleY + 56;
        int buttonW = Math.min(340, cardW - 120);
        int buttonH = 44;
        int buttonX = cardX + (cardW - buttonW) / 2;
        int firstButtonY = separatorY + 38;
        int gap = 16;

        Rectangle startButton = new Rectangle(buttonX, firstButtonY, buttonW, buttonH);
        Rectangle tutorialButton = new Rectangle(buttonX, firstButtonY + (buttonH + gap), buttonW, buttonH);
        Rectangle horrorButton = new Rectangle(buttonX, firstButtonY + 2 * (buttonH + gap), buttonW, buttonH);
        Rectangle quitButton = new Rectangle(buttonX, firstButtonY + 3 * (buttonH + gap), buttonW, buttonH);
        return new MenuLayout(cardX, cardY, cardW, cardH, titleY, separatorY,
                startButton, tutorialButton, horrorButton, quitButton);
    }


    void drawMenuMist(Graphics2D g2, int w, int h, long now) {
        Graphics2D mist = (Graphics2D) g2.create();
        mist.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int layer = 0; layer < 4; layer++) {
            int blobs = 4 + layer;
            float alpha = 0.035f + layer * 0.015f;
            mist.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            Color tint = new Color(190 - layer * 12, 186 - layer * 14, 180 - layer * 16);
            mist.setColor(tint);
            for (int i = 0; i < blobs; i++) {
                double drift = ((now * (0.018 + layer * 0.006)) + i * 180.0) % (w + 380.0) - 220.0;
                double wave = Math.sin(now / (1800.0 + layer * 280.0) + i * 0.8 + layer) * (24 + layer * 14);
                int cloudW = 220 + layer * 90 + (i % 3) * 42;
                int cloudH = 70 + layer * 18 + (i % 2) * 12;
                int x = (int) drift;
                int y = (int) (h * (0.16 + layer * 0.18) + wave + i * 18);
                mist.fillRoundRect(x, y, cloudW, cloudH, cloudH, cloudH);
                mist.fillOval(x - 28, y + 8, cloudW / 2, cloudH);
                mist.fillOval(x + cloudW / 3, y - 10, cloudW / 2, cloudH + 18);
            }
        }
        mist.dispose();
    }


private void drawMenuPill(Graphics2D g2, Rectangle bounds, String key, String label,
            Color fill, Color textColor, boolean hovered, float pulse) {
        Graphics2D pill = (Graphics2D) g2.create();
        pill.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int arc = 24;
        pill.setColor(new Color(0, 0, 0, hovered ? 110 : 82));
        pill.fillRoundRect(bounds.x + 4, bounds.y + 5, bounds.width, bounds.height, arc, arc);
        pill.setPaint(new GradientPaint(
                bounds.x, bounds.y, brighten(fill, hovered ? 0.12f : 0.04f),
                bounds.x, bounds.y + bounds.height, darken(fill, 0.18f)));
        pill.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc);

        if (hovered) {
            pill.setColor(new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(),
                    24 + (int) (48 * pulse)));
            pill.setStroke(new BasicStroke(6f));
            pill.drawRoundRect(bounds.x - 1, bounds.y - 1, bounds.width + 2, bounds.height + 2, arc, arc);
        }

        pill.setColor(new Color(255, 245, 224, 24));
        pill.fillRoundRect(bounds.x + 10, bounds.y + 6, bounds.width - 20, 11, 18, 18);
        pill.setColor(brighten(textColor, 0.10f));
        pill.setStroke(new BasicStroke(1.6f));
        pill.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc);

        pill.setFont(new Font("DialogInput", Font.BOLD, 15));
        FontMetrics keyFm = pill.getFontMetrics();
        int badgeW = Math.max(44, keyFm.stringWidth(key) + 16);
        int badgeH = bounds.height - 12;
        int badgeX = bounds.x + 10;
        int badgeY = bounds.y + 6;
        pill.setColor(new Color(8, 7, 5, 145));
        pill.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 16, 16);
        pill.setColor(new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 180));
        pill.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 16, 16);
        pill.setColor(textColor);
        pill.drawString(key, badgeX + (badgeW - keyFm.stringWidth(key)) / 2,
                badgeY + (badgeH - keyFm.getHeight()) / 2 + keyFm.getAscent());

        pill.setFont(new Font("Serif", Font.BOLD, 22));
        FontMetrics labelFm = pill.getFontMetrics();
        pill.setColor(new Color(0, 0, 0, 140));
        pill.drawString(label, badgeX + badgeW + 16, bounds.y + bounds.height / 2 + labelFm.getAscent() / 2 - 1);
        pill.setColor(textColor);
        pill.drawString(label, badgeX + badgeW + 15, bounds.y + bounds.height / 2 + labelFm.getAscent() / 2 - 2);
        pill.dispose();
    }


private Color brighten(Color color, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        int r = color.getRed() + Math.round((255 - color.getRed()) * amount);
        int g = color.getGreen() + Math.round((255 - color.getGreen()) * amount);
        int b = color.getBlue() + Math.round((255 - color.getBlue()) * amount);
        return new Color(Math.min(255, r), Math.min(255, g), Math.min(255, b), color.getAlpha());
    }


private Color darken(Color color, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        int r = Math.round(color.getRed() * (1f - amount));
        int g = Math.round(color.getGreen() * (1f - amount));
        int b = Math.round(color.getBlue() * (1f - amount));
        return new Color(Math.max(0, r), Math.max(0, g), Math.max(0, b), color.getAlpha());
    }


    void drawMenuTorchFlicker(Graphics2D g2, long now) {
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


}
