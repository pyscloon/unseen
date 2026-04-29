package unseen.ui.gamepanel;

import unseen.entities.Enemy;
import unseen.entities.Player;
import unseen.items.Shuriken;
import unseen.map.Tile;
import unseen.ui.GamePanel;
import unseen.utils.Constants;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Random;

class OverlayRenderer {
    private final GamePanel panel;
    private final LevelManager levelManager;
    private final WorldRenderer worldRenderer;
    private final MenuRenderer menuRenderer;

    OverlayRenderer(GamePanel panel, LevelManager levelManager,
            WorldRenderer worldRenderer, MenuRenderer menuRenderer) {
        this.panel = panel;
        this.levelManager = levelManager;
        this.worldRenderer = worldRenderer;
        this.menuRenderer = menuRenderer;
    }

    void drawIntroScreen(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = panel.getWidth();
        int h = panel.getHeight();
        long now = System.currentTimeMillis();

        Graphics2D bg = (Graphics2D) g2.create();
        double panX = Math.sin(now / 3300.0) * 12.0 + Math.cos(now / 1700.0) * 4.5;
        double panY = Math.cos(now / 4100.0) * 8.0 + Math.sin(now / 2100.0) * 3.0;
        double zoom = 1.06 + 0.022 * Math.sin(now / 4700.0) + 0.012 * Math.cos(now / 2600.0);
        bg.translate(panX - (w * (zoom - 1.0)) / 2.0, panY - (h * (zoom - 1.0)) / 2.0);
        bg.scale(zoom, zoom);
        worldRenderer.drawMap(bg);
        menuRenderer.drawMenuTorchFlicker(bg, now);
        bg.dispose();

        menuRenderer.drawMenuMist(g2, w, h, now);

        g2.setColor(new Color(0, 0, 0, 182));
        g2.fillRect(0, 0, w, h);

        RadialGradientPaint vignette = new RadialGradientPaint(
                w / 2f, h / 2f, Math.max(w, h) * 0.82f,
                new float[] { 0.08f, 0.54f, 1.0f },
                new Color[] {
                        new Color(0, 0, 0, 0),
                        new Color(8, 7, 5, 132),
                        new Color(0, 0, 0, 248)
                });
        g2.setPaint(vignette);
        g2.fillRect(0, 0, w, h);

        int cardW = Math.max(760, Math.min(940, w - 56));
        int cardH = Math.max(560, Math.min(660, h - 36));
        int cardX = (w - cardW) / 2;
        int cardY = (h - cardH) / 2;

        RoundRectangle2D.Float card = new RoundRectangle2D.Float(cardX, cardY, cardW, cardH, 40, 40);
        g2.setPaint(new GradientPaint(
                cardX, cardY, new Color(24, 18, 15, 212),
                cardX, cardY + cardH, new Color(8, 7, 6, 236)));
        g2.fill(card);
        g2.setColor(new Color(255, 245, 220, 16));
        g2.fillRoundRect(cardX + 18, cardY + 18, cardW - 36, 64, 30, 30);
        g2.setColor(new Color(214, 164, 70, 180));
        g2.setStroke(new BasicStroke(2.2f));
        g2.draw(card);
        g2.setColor(new Color(104, 72, 28, 140));
        g2.setStroke(new BasicStroke(1.1f));
        g2.drawRoundRect(cardX + 8, cardY + 8, cardW - 16, cardH - 16, 32, 32);

        String chapter = "PROLOGUE";
        g2.setFont(new Font("DialogInput", Font.BOLD, 16));
        FontMetrics chapterFm = g2.getFontMetrics();
        g2.setColor(new Color(237, 192, 94, 226));
        g2.drawString(chapter, w / 2 - chapterFm.stringWidth(chapter) / 2, cardY + 42);

        String title = "THE LEGEND OF 205, THE UNSEEN";
        g2.setFont(new Font("Serif", Font.BOLD, 38));
        FontMetrics titleFm = g2.getFontMetrics();
        int titleW = titleFm.stringWidth(title);
        int titleX = (w - titleW) / 2;
        int titleY = cardY + 84;

        g2.setColor(new Color(0, 0, 0, 220));
        g2.drawString(title, titleX + 3, titleY + 3);
        g2.setColor(new Color(78, 40, 8, 180));
        g2.drawString(title, titleX + 1, titleY + 1);
        g2.setPaint(new GradientPaint(
                titleX, titleY - 40, new Color(255, 231, 150),
                titleX, titleY + 8, new Color(241, 143, 44)));
        g2.drawString(title, titleX, titleY);

        g2.setFont(new Font("Serif", Font.ITALIC, 19));
        String subtitle = "A voice rises from the buried dark.";
        int subtitleW = g2.getFontMetrics().stringWidth(subtitle);
        g2.setColor(new Color(188, 153, 101, 228));
        g2.drawString(subtitle, (w - subtitleW) / 2, titleY + 34);

        int sepY = cardY + 108;
        int sepInset = 88;
        g2.setColor(new Color(120, 79, 26, 190));
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawLine(cardX + sepInset, sepY, cardX + cardW - sepInset, sepY);
        g2.setColor(new Color(240, 199, 94, 120));
        g2.drawLine(cardX + sepInset, sepY - 2, cardX + cardW - sepInset, sepY - 2);
        int dx = w / 2;
        int ds = 7;
        int[] dpx = { dx, dx + ds, dx, dx - ds };
        int[] dpy = { sepY - ds, sepY, sepY + ds, sepY };
        g2.setColor(new Color(211, 165, 61, 232));
        g2.fillPolygon(dpx, dpy, 4);

        int textX = cardX + 56;
        int textY = cardY + 142;
        int textW = cardW - 112;
        int textBottom = cardY + cardH - 84;

        Font bodyFont = fitIntroBodyFont(g2, textW, textBottom - textY);
        g2.setFont(bodyFont);
        FontMetrics bodyFm = g2.getFontMetrics();
        List<String> lines = buildIntroLines(textW, bodyFm, panel.getIntroVisibleTicks());
        int lineHeight = bodyFm.getHeight() + 1;
        int paraGap = 10;

        int drawY = textY;
        for (String line : lines) {
            if (drawY > textBottom) {
                break;
            }
            if (line.isEmpty()) {
                drawY += paraGap;
                continue;
            }
            g2.setColor(new Color(0, 0, 0, 150));
            g2.drawString(line, textX + 2, drawY + 2);
            g2.setColor(new Color(232, 220, 198, 232));
            g2.drawString(line, textX, drawY);
            drawY += lineHeight;
        }

        if (!panel.isIntroFullyRevealed()) {
            int cursorY = Math.min(textBottom - 8, drawY + 4);
            float cursorPulse = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(now / 180.0));
            g2.setColor(new Color(255, 194, 92, (int) (160 * cursorPulse)));
            g2.fillRoundRect(textX, cursorY, 14, 3, 3, 3);
        }

        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        String prompt = panel.isIntroFullyRevealed()
                ? "SPACE / ENTER / CLICK  -  descend to the menu"
                : "SPACE / ENTER / CLICK  -  reveal the whole tale    ESC  -  skip";
        int promptW = g2.getFontMetrics().stringWidth(prompt);
        float promptPulse = 0.55f + 0.45f * (0.5f + 0.5f * (float) Math.sin(now / 360.0));
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect((w - promptW) / 2 - 18, cardY + cardH - 54, promptW + 36, 32, 18, 18);
        g2.setColor(new Color(228, 188, 100, (int) (185 + 45 * promptPulse)));
        g2.drawString(prompt, (w - promptW) / 2, cardY + cardH - 33);

        g2.dispose();
    }

    private Font fitIntroBodyFont(Graphics2D g2, int maxWidth, int maxHeight) {
        for (int size = 30; size >= 20; size--) {
            Font font = new Font("Serif", Font.PLAIN, size);
            FontMetrics fm = g2.getFontMetrics(font);
            List<String> fullLines = buildIntroLines(maxWidth, fm, panel.getIntroTotalTicks());
            if (measureIntroHeight(fullLines, fm) <= maxHeight) {
                return font;
            }
        }
        return new Font("Serif", Font.PLAIN, 20);
    }

    private int measureIntroHeight(List<String> lines, FontMetrics fm) {
        int lineHeight = fm.getHeight() + 1;
        int paraGap = 10;
        int height = 0;
        for (String line : lines) {
            height += line.isEmpty() ? paraGap : lineHeight;
        }
        return height;
    }

    private List<String> buildIntroLines(int maxWidth, FontMetrics fm, int visibleTicks) {
        List<String> lines = new java.util.ArrayList<>();
        int remainingTicks = visibleTicks;
        int paragraphPauseTicks = panel.getIntroParagraphPauseTicks();

        for (String paragraph : panel.getIntroNarration()) {
            if (remainingTicks <= 0) {
                break;
            }

            if (paragraph == null || paragraph.isEmpty()) {
                continue;
            }

            int visible = Math.min(remainingTicks, paragraph.length());
            if (visible <= 0) {
                break;
            }

            String visibleText = paragraph.substring(0, visible);
            wrapParagraph(lines, visibleText, maxWidth, fm);
            remainingTicks -= visible;

            if (visible == paragraph.length() && remainingTicks > 0) {
                if (remainingTicks >= paragraphPauseTicks) {
                    lines.add("");
                    remainingTicks -= paragraphPauseTicks;
                } else {
                    break;
                }
            }
        }

        return lines;
    }

    private void wrapParagraph(List<String> lines, String text, int maxWidth, FontMetrics fm) {
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (current.length() > 0 && fm.stringWidth(candidate) > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                current.setLength(0);
                current.append(candidate);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
    }

    void drawShurikenAimOverlay(Graphics g) {
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
        boolean blockedImmediately = false;
        int travelTiles = 0;
        for (int i = 1; i <= 5; i++) {
            int tx = px + dx * i;
            int ty = py + dy * i;
            if (tx < 0 || tx >= Constants.GRID_WIDTH || ty < 0 || ty >= Constants.GRID_HEIGHT)
                break;
            if (map.getTile(tx, ty) == unseen.map.Tile.WALL) {
                g2.setColor(new Color(200, 50, 50, 120));
                g2.fillRect(tx * ts, ty * ts, ts, ts);
                g2.setColor(new Color(255, 90, 90, 220));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRect(tx * ts, ty * ts, ts, ts);
                blockedImmediately = i == 1;
                break;
            }
            travelTiles = i;

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
        int endCx = startCx + dx * Math.max(1, travelTiles) * ts;
        int endCy = startCy + dy * Math.max(1, travelTiles) * ts;
        g2.setColor(blockedImmediately ? new Color(255, 80, 80, 160)
                : (hitEnemy ? new Color(255, 80, 80, 170) : new Color(180, 240, 255, 130)));
        float[] dash = { 4f, 4f };
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
        g2.drawLine(startCx, startCy, endCx, endCy);

        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(180, 240, 255, 80));
        g2.drawOval(startCx - unseen.items.Shuriken.RANGE * ts, startCy - unseen.items.Shuriken.RANGE * ts,
                unseen.items.Shuriken.RANGE * ts * 2, unseen.items.Shuriken.RANGE * ts * 2);

        String msg = blockedImmediately
                ? "Blocked  |  WASD aim  |  Esc cancel"
                : ((hitEnemy ? "Will hit enemy!  " : "") + "WASD aim  |  Space/Enter throw  |  Esc cancel");
        g2.setFont(new Font("Arial", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        int msgW = fm.stringWidth(msg);
        int bx = (panel.getWidth() - msgW) / 2 - 12;
        int by = panel.getHeight() - 42;
        g2.setColor(new Color(20, 20, 20, 190));
        g2.setStroke(new BasicStroke(1f));
        g2.fillRoundRect(bx, by, msgW + 24, 28, 10, 10);
        g2.setColor(blockedImmediately ? new Color(255, 130, 130)
                : (hitEnemy ? new Color(255, 160, 160) : new Color(180, 240, 255)));
        g2.drawString(msg, bx + 12, by + 20);
    }

    void drawTargetingOverlay(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int ts = Constants.TILE_SIZE;

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(0, 0, panel.getWidth(), panel.getHeight());

        int mouseGridX = panel.getMouseGridX();
        int mouseGridY = panel.getMouseGridY();
        Player player = levelManager.getPlayer();
        int playerCx = player.getX() * ts + ts / 2;
        int playerCy = player.getY() * ts + ts / 2;

        if (panel.isTargetingGrapplingHook()) {
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(120, 200, 255, 95));
            int range = unseen.items.GrapplingHook.RANGE * ts;
            g2.drawOval(playerCx - range, playerCy - range, range * 2, range * 2);

            for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
                for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                    int dx = x - player.getX();
                    int dy = y - player.getY();
                    if (dx * dx + dy * dy > unseen.items.GrapplingHook.RANGE * unseen.items.GrapplingHook.RANGE) {
                        continue;
                    }
                    if (levelManager.getMap().getTile(x, y) != unseen.map.Tile.WALL) {
                        continue;
                    }
                    boolean validWall = panel.isValidGrapplingHookTarget(x, y);
                    g2.setColor(validWall ? new Color(80, 180, 255, 42) : new Color(255, 60, 50, 24));
                    g2.fillRect(x * ts, y * ts, ts, ts);
                }
            }
        }

        boolean validTile = mouseGridX >= 0 && mouseGridY >= 0
                && mouseGridX < Constants.GRID_WIDTH && mouseGridY < Constants.GRID_HEIGHT
                && (panel.isTargetingGrapplingHook()
                        ? panel.isValidGrapplingHookTarget(mouseGridX, mouseGridY)
                        : levelManager.getMap().isPassable(mouseGridX, mouseGridY));

        int tx = mouseGridX * ts;
        int ty = mouseGridY * ts;

        if (panel.isTargetingShuriken())
            drawShurikenAimOverlay(g);

        if (validTile) {
            if (panel.isTargetingGrapplingHook()) {
                int[] landing = panel.getGrappleLandingPreview(mouseGridX, mouseGridY);
                g2.setColor(new Color(120, 200, 255, 130));
                g2.fillRect(tx, ty, ts, ts);
                g2.setColor(new Color(180, 230, 255, 220));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRect(tx, ty, ts, ts);
                int cx = tx + ts / 2, cy = ty + ts / 2;
                g2.drawOval(cx - ts / 4, cy - ts / 4, ts / 2, ts / 2);
                g2.drawLine(cx, cy, cx, cy + ts / 3);
                if (landing != null) {
                    int lx = landing[0] * ts;
                    int ly = landing[1] * ts;
                    int lcX = lx + ts / 2;
                    int lcY = ly + ts / 2;
                    g2.setColor(new Color(120, 255, 190, 120));
                    g2.fillRect(lx, ly, ts, ts);
                    g2.setColor(new Color(150, 255, 210, 235));
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawRect(lx, ly, ts, ts);
                    g2.setColor(new Color(180, 230, 255, 190));
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(playerCx, playerCy, cx, cy);
                    g2.drawLine(cx, cy, lcX, lcY);
                }
            } else if (panel.isTargetingFlare()) {
                g2.setColor(new Color(255, 240, 100, 130));
                g2.fillRect(tx, ty, ts, ts);
                g2.setColor(new Color(255, 255, 150, 220));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRect(tx, ty, ts, ts);
                int cx = tx + ts / 2, cy = ty + ts / 2;
                int flareR = 5 * ts;
                g2.setColor(new Color(255, 245, 120, 55));
                g2.fillOval(cx - flareR, cy - flareR, flareR * 2, flareR * 2);
                g2.setColor(new Color(255, 255, 150, 200));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx - flareR, cy - flareR, flareR * 2, flareR * 2);
                g2.drawLine(cx - ts / 2, cy, cx + ts / 2, cy);
                g2.drawLine(cx, cy - ts / 2, cx, cy + ts / 2);
            } else {
                g2.setColor(new Color(255, 200, 50, 130));
                g2.fillRect(tx, ty, ts, ts);
                g2.setColor(new Color(255, 220, 80, 220));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRect(tx, ty, ts, ts);
                int cx = tx + ts / 2, cy = ty + ts / 2;
                int noiseR = 6 * ts;
                g2.setColor(new Color(255, 210, 70, 45));
                g2.fillOval(cx - noiseR, cy - noiseR, noiseR * 2, noiseR * 2);
                g2.setColor(new Color(255, 220, 80, 200));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx - noiseR, cy - noiseR, noiseR * 2, noiseR * 2);
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
                    : "Need straight wall with clear path  |  Esc cancel";
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

    void drawJumpscare(Graphics2D g2) {
        int w = panel.getWidth();
        int h = panel.getHeight();
        Random rand = new Random();

        // 1. Deep Void Background
        g2.setColor(new Color(5, 2, 2));
        g2.fillRect(0, 0, w, h);

        // 2. Grainy Static / Noise
        for (int i = 0; i < 400; i++) {
            int alpha = rand.nextInt(40);
            g2.setColor(new Color(rand.nextInt(50), 0, 0, alpha));
            g2.fillRect(rand.nextInt(w), rand.nextInt(h), 2, 2);
        }

        int centerX = w / 2;
        int centerY = h / 2;

        // 3. Glitch - Shift slices of the screen
        if (rand.nextDouble() < 0.7) {
            int slices = 3 + rand.nextInt(8);
            for (int i = 0; i < slices; i++) {
                int sy = rand.nextInt(h);
                int sh = 5 + rand.nextInt(30);
                int offset = rand.nextInt(40) - 20;
                g2.copyArea(0, sy, w, sh, offset, 0);
            }
        }

        // 4. The "Many Eyes" - Tiny white pinpricks in the dark
        g2.setColor(new Color(255, 255, 255, 180));
        for (int i = 0; i < 15; i++) {
            int ex = rand.nextInt(w);
            int ey = rand.nextInt(h);
            int es = 1 + rand.nextInt(3);
            g2.fillOval(ex, ey, es, es);
        }

        // 5. The Main Face - Distorted and Melting
        // Left Eye (Void)
        drawDistortedEye(g2, centerX - 160 + rand.nextInt(15), centerY - 120 + rand.nextInt(15), 140, rand);
        // Right Eye (Void)
        drawDistortedEye(g2, centerX + 160 - rand.nextInt(15), centerY - 120 + rand.nextInt(15), 140, rand);

        // 6. The Mouth (Needle Teeth)
        int mouthW = 240 + rand.nextInt(60);
        int mouthH = 350 + rand.nextInt(150);
        g2.setColor(new Color(0, 0, 0, 255));
        g2.fillOval(centerX - mouthW / 2, centerY + 20, mouthW, mouthH);

        // Jagged Teeth
        g2.setColor(new Color(200, 200, 180, 180));
        for (int i = 0; i < 20; i++) {
            int tx = centerX - mouthW / 2 + (mouthW * i / 20);
            int ty = centerY + 40 + rand.nextInt(20);
            int tw = 4 + rand.nextInt(6);
            int th = 15 + rand.nextInt(40);
            // Top teeth
            g2.fillPolygon(new int[] { tx, tx + tw, tx + tw / 2 }, new int[] { ty, ty, ty + th }, 3);
            // Bottom teeth
            int bty = centerY + 40 + mouthH - 60 - rand.nextInt(20);
            g2.fillPolygon(new int[] { tx, tx + tw, tx + tw / 2 }, new int[] { bty, bty, bty - th }, 3);
        }

        // 7. Blood-like Drips
        g2.setColor(new Color(150, 0, 0, 200));
        for (int i = 0; i < 8; i++) {
            int dx = centerX - 200 + rand.nextInt(400);
            int dy = centerY - 50 + rand.nextInt(100);
            int dw = 2 + rand.nextInt(4);
            int dh = 40 + rand.nextInt(150);
            g2.fillRect(dx, dy, dw, dh);
        }

        // 8. Sudden Flash of "HIM"
        if (rand.nextDouble() < 0.15) {
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRect(0, 0, w, h);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Monospaced", Font.BOLD, 120));
            g2.drawString("SAW YOU", centerX - 250, centerY);
        }

        // 9. Vignette
        RadialGradientPaint vignette = new RadialGradientPaint(
                centerX, centerY, (float) Math.max(w, h) * 0.7f,
                new float[] { 0.0f, 1.0f },
                new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 255) });
        g2.setPaint(vignette);
        g2.fillRect(0, 0, w, h);
    }

    private void drawDistortedEye(Graphics2D g2, int x, int y, int baseSize, Random rand) {
        // Outer glow
        g2.setColor(new Color(180, 0, 0, 40 + rand.nextInt(40)));
        g2.fillOval(x - baseSize / 2 - 10, y - baseSize / 2 - 10, baseSize + 20, baseSize + 20);

        // Eye void
        g2.setColor(Color.BLACK);
        g2.fillOval(x - baseSize / 2, y - baseSize / 2, baseSize, baseSize + rand.nextInt(40));

        // Pupil (pulsing red)
        g2.setColor(new Color(255, 0, 0, 150 + rand.nextInt(100)));
        int pSize = 10 + rand.nextInt(30);
        g2.fillOval(x - pSize / 2 + rand.nextInt(10) - 5, y - pSize / 2 + rand.nextInt(10) - 5, pSize, pSize);
    }

}
