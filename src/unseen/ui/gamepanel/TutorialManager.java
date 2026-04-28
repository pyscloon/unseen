package unseen.ui.gamepanel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import unseen.utils.AssetLoader;
import unseen.ui.GamePanel;

public class TutorialManager {

    private final GamePanel panel;

    public enum PageType {
        INTRO, ITEMS, ENEMIES, HORROR, CONTROLS
    }

    public static class TutorialPage {
        public final PageType type;
        public final String title;
        public final String subtitle;
        public final String[] body;
        public final List<EntryRow> entries;

        public TutorialPage(PageType type, String title, String subtitle, String... body) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.body = body;
            this.entries = new ArrayList<>();
        }

        public TutorialPage(PageType type, String title, String subtitle, List<EntryRow> entries) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
            this.body = new String[0];
            this.entries = entries;
        }
    }

    public static class EntryRow {
        public final String name;
        public final String key;
        public final Color accent;
        public final String[] lines;
        public final Image icon;

        public EntryRow(String name, String key, Color accent, Image icon, String... lines) {
            this.name = name;
            this.key = key;
            this.accent = accent;
            this.icon = icon;
            this.lines = lines;
        }
    }

    private final List<TutorialPage> PAGES;
    private boolean active = false;
    private int pageIdx = 0;

    public TutorialManager(GamePanel panel) {
        this.panel = panel;
        this.PAGES = buildPages();
    }

    private List<TutorialPage> buildPages() {
        List<TutorialPage> pages = new ArrayList<>();
        AssetLoader assets = AssetLoader.get();

        pages.add(new TutorialPage(
                PageType.INTRO,
                "UNSEEN",
                "A stealth roguelike -- stay in the shadows, survive the floors.",
                "You are a lone operative moving through a series of darkened",
                "procedurally generated floors. Guards patrol every corridor.",
                " ",
                "Your goal is simple: reach the EXIT tile on each floor",
                "without being caught. There are no second chances -- if an",
                "enemy steps onto your tile, the run is over.",
                " ",
<<<<<<< Updated upstream
                "Use items, shadows, and cunning to slip past every threat."));
=======
                "Use items, shadows, and cunning to slip past every threat.",
                " ",
                "WATCH YOUR STEP: Environmental hazards like PUDDLES",
                "can alert nearby enemies if you step on them!"));
>>>>>>> Stashed changes

        pages.add(new TutorialPage(
                PageType.CONTROLS,
                "CONTROLS",
                "Every action costs one turn. Enemies move after you.",
                "W / A / S / D   --   Move up / left / down / right",
                "E               --   Pick up item on current tile",
<<<<<<< Updated upstream
                "1               --   Use Noise Maker  (click tile to target)",
                "2               --   Use Smoke Bomb   (instant, centred on you)",
                "3               --   Use Flare / Lantern  (click tile to target)",
                "4               --   Use Shuriken  (WASD to aim, Space to throw)",
                "5               --   Use Holy Cross (Purify Floor - Horror Mode Only)",
                "ESC             --   Cancel targeting / Pause / Return to Menu",
                "P               --   Pause / Resume (Any key also resumes)",
=======
                "1-6             --   Use item in corresponding slot",
                "ESC             --   Cancel targeting / Pause",
                "P               --   Pause / Resume",
>>>>>>> Stashed changes
                "M               --   Toggle Music",
                "N               --   Toggle Sound Effects",
                "R               --   Restart  (on death screen)"));

        List<EntryRow> itemRows1 = new ArrayList<>();
        itemRows1.add(new EntryRow(
                "Noise Maker", "[1]",
                new Color(255, 210, 60), assets.noiseMaker,
                "Throw it to any visible floor tile.",
                "Enemies within range will investigate the sound,",
                "drawing them away from their patrol route."));
        itemRows1.add(new EntryRow(
                "Smoke Bomb", "[2]",
                new Color(160, 190, 220), assets.smokeBomb,
                "Instantly obscures your current position.",
                "Enemies cannot see you while you are inside smoke.",
                "Lasts for several turns. Useful for quick escapes."));
        itemRows1.add(new EntryRow(
                "Lantern / Flare", "[3]",
                new Color(255, 240, 150), assets.lantern,
                "Illuminates a large area around the target tile.",
                "Stay out of the light! Enemies see much further in",
                "lit areas than they do in the dark."));
        itemRows1.add(new EntryRow(
                "Shuriken", "[4]",
<<<<<<< Updated upstream
                new Color(180, 220, 255),
                "Press 4 to enter aim mode. Use WASD to set direction.",
                "Press Space or Enter to throw. Travels up to 5 tiles",
                "in a straight line and silently eliminates the first enemy hit.",
                "Wall stops the shuriken. Only one throw per shuriken."));
        itemRows.add(new EntryRow(
                "Holy Cross", "[5]",
                new Color(255, 255, 180),
                "A sacred artifact that only functions in HORROR MODE.",
                "Instantly purifies the floor: banishes the Stalker,",
                "cleanses blood, and reverts atmosphere to Normal Mode.",
                "Extremely rare. Use it when the darkness becomes too much."));
        // -- END ITEMS --

        pages.add(new TutorialPage(
                PageType.ITEMS,
                "ITEMS",
                "Each item is consumed on use. Pick up more on each floor.",
                itemRows));
=======
                new Color(180, 180, 190), assets.shuriken,
                "A silent throwing weapon. Aim with WASD.",
                "Hits the first enemy in its path, removing them",
                "instantly. Limited supply -- use wisely."));
        pages.add(new TutorialPage(PageType.ITEMS, "ITEMS (1/2)", "Tools for distraction and survival.", itemRows1));

        List<EntryRow> itemRows2 = new ArrayList<>();
        itemRows2.add(new EntryRow(
                "Grappling Hook", "[5]",
                new Color(100, 200, 255), assets.grapplingHook,
                "Target a wall within 6 tiles to pull yourself to it.",
                "Allows rapid movement across rooms and",
                "over hazards. Essential for mobility."));
        itemRows2.add(new EntryRow(
                "Holy Cross", "[6]",
                new Color(255, 255, 200), assets.cross,
                "HORROR MODE ONLY. Purifies the current floor.",
                "Banishes darkness and reveals the exit.",
                "Extremely rare and powerful."));
        pages.add(new TutorialPage(PageType.ITEMS, "ITEMS (2/2)", "Advanced equipment.", itemRows2));

        List<EntryRow> envRows = new ArrayList<>();
        envRows.add(new EntryRow(
                "Campfire", "Rest",
                new Color(255, 120, 40), assets.campfire,
                "Stand on a campfire for 5 turns to REST.",
                "Restoring +1 HP. Can only be used ONCE",
                "every 2 floors. High risk, high reward."));
        envRows.add(new EntryRow(
                "Puddle", "Hazard",
                new Color(100, 150, 255), assets.puddle,
                "Stepping here creates a loud SPLASH.",
                "Enemies nearby will hear you and investigate.",
                "Watch your step in Normal Mode."));
        pages.add(new TutorialPage(PageType.ENVIRONMENT, "ENVIRONMENT", "Interactable objects and hazards.", envRows));
>>>>>>> Stashed changes

        List<EntryRow> enemyRows = new ArrayList<>();
        enemyRows.add(new EntryRow(
                "Sentry Guard", "Static",
                new Color(220, 60, 60), assets.sentry,
                "Stays in one spot, scanning its surroundings.",
                "High vision range. If it spots you, it alerts",
                "all nearby guards to your position."));
        enemyRows.add(new EntryRow(
                "Patrol Guard", "Moving",
                new Color(200, 100, 40), assets.patrol,
                "Follows a fixed route through the floor.",
                "Move when they are looking away. They move",
                "one tile every time you take a turn."));
        pages.add(new TutorialPage(PageType.ENEMIES, "ENEMIES", "The threats that dwell in the dark.", enemyRows));

        List<EntryRow> horrorRows = new ArrayList<>();
        horrorRows.add(new EntryRow(
                "Shadow Figure", "Horror",
                new Color(180, 0, 0), assets.horrorFloor,
                "In Horror Mode, the environment is hostile.",
                "Visions of dread and bloodied floors",
                "will test your sanity."));
        pages.add(new TutorialPage(PageType.HORROR, "HORROR MODE", "A deeper, darker challenge.", horrorRows));

        return pages;
    }

    public boolean isActive() { return active; }
    public void reset() { pageIdx = 0; active = true; }
    public void dismiss() { active = false; pageIdx = 0; }
    public void nextPage() { if (pageIdx < PAGES.size() - 1) pageIdx++; else dismiss(); }
    public void prevPage() { if (pageIdx > 0) pageIdx--; }
    public TutorialPage currentPageData() { return PAGES.get(pageIdx); }

    public void draw(Graphics g, int panelW, int panelH) {
        if (!active) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        long now = System.currentTimeMillis();

        // Darken background
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, panelW, panelH);

        // Main card
        int cardW = panelW - 120;
        int cardH = panelH - 140;
        int cardX = (panelW - cardW) / 2;
        int cardY = (panelH - cardH) / 2;

        // Shadow
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRoundRect(cardX + 8, cardY + 8, cardW, cardH, 20, 20);

        // Glassy body
        g2.setColor(new Color(20, 16, 12, 235));
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);

        // Border
        g2.setColor(new Color(110, 80, 30, 220));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(cardX, cardY, cardW, cardH, 20, 20);
        g2.setColor(new Color(60, 45, 20, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(cardX + 6, cardY + 6, cardW - 12, cardH - 12, 16, 16);

        TutorialPage page = currentPageData();

        // Pulsing dots
        int dotY = cardY + 22;
        int dotR = 5;
        int dotGap = 20;
        int dotsW = PAGES.size() * dotGap - (dotGap - dotR * 2);
        int dotStartX = cardX + (cardW - dotsW) / 2;
        float dotPulse = (float)(0.4 + 0.3 * Math.sin(now / 400.0));
        
        for (int i = 0; i < PAGES.size(); i++) {
            if (i == pageIdx) {
                int glow = (int)(15 * dotPulse);
                g2.setColor(new Color(255, 210, 80, 100));
                g2.fillOval(dotStartX - glow/2, dotY - glow/2, dotR*2 + glow, dotR*2 + glow);
                g2.setColor(new Color(255, 230, 120));
            } else {
                g2.setColor(new Color(80, 60, 30, 180));
            }
            g2.fillOval(dotStartX, dotY, dotR * 2, dotR * 2);
            dotStartX += dotGap;
        }

        // Title
        int titleY = cardY + 65;
        g2.setFont(new Font("Serif", Font.BOLD, 34));
        FontMetrics tfm = g2.getFontMetrics();
        int tw = tfm.stringWidth(page.title);
        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(page.title, cardX + (cardW - tw) / 2 + 2, titleY + 2);
        g2.setColor(new Color(255, 220, 100));
        g2.drawString(page.title, cardX + (cardW - tw) / 2, titleY);

        // Subtitle
        int subtitleY = titleY + 25;
        g2.setFont(new Font("SansSerif", Font.ITALIC, 15));
        FontMetrics sfm = g2.getFontMetrics();
        int sw = sfm.stringWidth(page.subtitle);
        g2.setColor(new Color(180, 160, 120));
        g2.drawString(page.subtitle, cardX + (cardW - sw) / 2, subtitleY);

        // Separator
        int divY = subtitleY + 15;
        g2.setColor(new Color(110, 80, 30, 140));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(cardX + 60, divY, cardX + cardW - 60, divY);

        int contentY = divY + 30;
        int contentMaxH = cardY + cardH - 80 - contentY;
        int contentX = cardX + 50;
        int contentW = cardW - 100;

        switch (page.type) {
            case INTRO:
            case CONTROLS:
                drawTextPage(g2, page, contentX, contentY, contentW, contentMaxH);
                break;
            default:
                drawEntryPage(g2, page, contentX, contentY, contentW, contentMaxH);
                break;
        }

        // Navigation Buttons
        int btnY = cardY + cardH - 60;
        int btnH = 36;
        int btnW = 130;

        if (pageIdx > 0) {
            drawPremiumButton(g2, cardX + 30, btnY, btnW, btnH, "< Previous",
                    new Color(40, 30, 20, 220), new Color(180, 160, 100));
        }

        String nextLabel = (pageIdx == PAGES.size() - 1) ? "Enter Dark >" : "Continue >";
        Color nextFill = (pageIdx == PAGES.size() - 1)
                ? new Color(80, 40, 20, 220)
                : new Color(50, 40, 30, 220);
        Color nextText = (pageIdx == PAGES.size() - 1)
                ? new Color(255, 140, 40)
                : new Color(220, 190, 100);
        drawPremiumButton(g2, cardX + cardW - btnW - 30, btnY, btnW, btnH, nextLabel, nextFill, nextText);

        g2.dispose();
    }

    private void drawTextPage(Graphics2D g2, TutorialPage page, int x, int y, int w, int maxH) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
        FontMetrics fm = g2.getFontMetrics();
        int lineH = fm.getHeight() + 4;
        int cy = y;
        for (String line : page.body) {
            if (cy + lineH > y + maxH) break;
            if (line.isBlank()) { cy += lineH / 2; continue; }
            g2.setColor(new Color(210, 195, 160));
            g2.drawString(line, x, cy + fm.getAscent());
            cy += lineH;
        }
    }

    private void drawEntryPage(Graphics2D g2, TutorialPage page, int x, int y, int w, int maxH) {
        int cy = y;
        int rowGap = 20;
        int iconSz = 40;
        int descIndent = x + iconSz + 18;

        for (EntryRow row : page.entries) {
            if (cy > y + maxH - 25) break;

            if (row.icon != null) {
                g2.setColor(new Color(row.accent.getRed(), row.accent.getGreen(), row.accent.getBlue(), 40));
                g2.fillRoundRect(x, cy, iconSz, iconSz, 8, 8);
                g2.drawImage(row.icon, x + 3, cy + 3, iconSz - 6, iconSz - 6, null);
                g2.setColor(new Color(row.accent.getRed(), row.accent.getGreen(), row.accent.getBlue(), 120));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x, cy, iconSz, iconSz, 8, 8);
            } else {
                g2.setColor(row.accent);
                g2.fillRect(x, cy, 6, iconSz);
            }

            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            FontMetrics bfm = g2.getFontMetrics();
            g2.setColor(new Color(240, 230, 200));
            g2.drawString(row.name, descIndent, cy + bfm.getAscent() - 2);

            if (!row.key.isEmpty()) {
                int nameW = bfm.stringWidth(row.name);
                g2.setFont(new Font("Monospaced", Font.BOLD, 11));
                FontMetrics kfm = g2.getFontMetrics();
                int keyW = kfm.stringWidth(row.key) + 12;
                int keyX = descIndent + nameW + 15;
                int keyY = cy - 2;
                int keyH = 18;
                g2.setColor(new Color(60, 45, 20, 220));
                g2.fillRoundRect(keyX, keyY, keyW, keyH, 6, 6);
                g2.setColor(new Color(110, 80, 30));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(keyX, keyY, keyW, keyH, 6, 6);
                g2.setColor(new Color(255, 210, 80));
                g2.drawString(row.key, keyX + 6, keyY + kfm.getAscent() + 1);
            }

            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            FontMetrics dfm = g2.getFontMetrics();
            int lcy = cy + bfm.getHeight() - 2;
            for (String line : row.lines) {
                if (lcy > y + maxH - 10) break;
                g2.setColor(new Color(165, 155, 140));
                g2.drawString(line, descIndent, lcy + dfm.getAscent());
                lcy += dfm.getHeight() + 2;
            }
            cy = Math.max(cy + iconSz, lcy) + rowGap;
        }
    }

    private void drawPremiumButton(Graphics2D g2, int x, int y, int w, int h, String label, Color fill, Color text) {
        g2.setColor(fill);
        g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(new Color(110, 80, 30, 180));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 12, 12);
        g2.setFont(new Font("Serif", Font.BOLD, 17));
        FontMetrics fm = g2.getFontMetrics();
        int lx = x + (w - fm.stringWidth(label)) / 2;
        int ly = y + (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(label, lx + 1, ly + 1);
        g2.setColor(text);
        g2.drawString(label, lx, ly);
    }

    public boolean handleClick(int mouseX, int mouseY, int panelW, int panelH) {
        if (!active) return false;
        int cardW = panelW - 120;
        int cardH = panelH - 140;
        int cardX = (panelW - cardW) / 2;
        int cardY = (panelH - cardH) / 2;
        int btnY = cardY + cardH - 60;
        int btnH = 36;
        int btnW = 130;

        int nextX = cardX + cardW - btnW - 30;
        if (mouseX >= nextX && mouseX <= nextX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            if (pageIdx == PAGES.size() - 1) { dismiss(); return false; }
            nextPage();
            return true;
        }

        if (pageIdx > 0) {
            int prevX = cardX + 30;
            if (mouseX >= prevX && mouseX <= prevX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                prevPage();
                return true;
            }
        }
        return true;
    }
}