package unseen.ui.gamepanel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TutorialManager {

    public enum PageType {
        INTRO, ITEMS, ENVIRONMENT, ENEMIES, HORROR, CONTROLS
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

        public EntryRow(String name, String key, Color accent, String... lines) {
            this.name = name;
            this.key = key;
            this.accent = accent;
            this.lines = lines;
        }
    }

    private static final List<TutorialPage> PAGES = buildPages();

    private static List<TutorialPage> buildPages() {
        List<TutorialPage> pages = new ArrayList<>();

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
                "Use items, shadows, and cunning to slip past every threat.",
                " ",
                "WATCH YOUR STEP: Environmental hazards like PUDDLES",
                "can alert nearby enemies if you step on them! (Normal Mode)"));

        pages.add(new TutorialPage(
                PageType.CONTROLS,
                "CONTROLS",
                "Every action costs one turn. Enemies move after you.",
                "W / A / S / D   --   Move up / left / down / right",
                "E               --   Pick up item on current tile",
                "1               --   Use Noise Maker  (click tile to target)",
                "2               --   Use Smoke Bomb   (instant, centred on you)",
                "3               --   Use Flare / Lantern  (click tile to target)",
                "4               --   Use Shuriken  (WASD to aim, Space to throw)",
                "5               --   Use Grappling Hook  (click wall within 6 tiles)",
                "6               --   Use Holy Cross (Purify Floor - Horror Mode Only)",
                "ESC             --   Cancel targeting / Pause / Return to Menu",
                "P               --   Pause / Resume (Any key also resumes)",
                "M               --   Toggle Music",
                "N               --   Toggle Sound Effects",
                "R               --   Restart  (on death screen)"));

        List<EntryRow> itemRows = new ArrayList<>();
        // -- ADD NEW ITEMS BELOW THIS LINE --
        itemRows.add(new EntryRow(
                "Noise Maker", "[1]",
                new Color(255, 210, 60),
                "Throw it to any visible floor tile.",
                "Enemies within range will investigate the sound,",
                "drawing them away from their patrol route."));
        itemRows.add(new EntryRow(
                "Smoke Bomb", "[2]",
                new Color(160, 190, 220),
                "Instantly detonates on your tile.",
                "Creates a smoke cloud that blocks enemy line-of-sight",
                "for several turns. Great for emergency cover."));
        itemRows.add(new EntryRow(
                "Flare / Lantern", "[3]",
                new Color(255, 240, 100),
                "Throw it to any visible floor tile.",
                "Illuminates a wide radius for many turns --",
                "useful for scouting ahead or confusing sentries."));
        itemRows.add(new EntryRow(
                "Shuriken", "[4]",
                new Color(180, 220, 255),
                "Press 4 to enter aim mode. Use WASD to set direction.",
                "Press Space or Enter to throw. Travels up to 5 tiles",
                "in a straight line and silently eliminates the first enemy hit.",
                "Wall stops the shuriken. Only one throw per shuriken."));
        itemRows.add(new EntryRow(
                "Grappling Hook", "[5]",
                new Color(120, 200, 255),
                "Fire a hook at any wall tile within 6 tiles (non-straight paths OK).",
                "You'll zip to the nearest floor tile next to the wall.",
                "Can bypass enemies, but zipping through them deals damage!"));
        itemRows.add(new EntryRow(
                "Holy Cross", "[6]",
                new Color(255, 255, 180),
                "A sacred artifact that only functions in HORROR MODE.",
                "Instantly purifies the floor: banishes the Stalker,",
                "cleanses blood, and reverts atmosphere to Normal Mode.",
                "Extremely rare. Use it when the darkness becomes too much."));
        // -- END ITEMS --

        List<EntryRow> itemRows1 = new ArrayList<>(itemRows.subList(0, 4));
        List<EntryRow> itemRows2 = new ArrayList<>(itemRows.subList(4, itemRows.size()));

        pages.add(new TutorialPage(
                PageType.ITEMS,
                "ITEMS (1/2)",
                "Each item is consumed on use. Pick up more on each floor.",
                itemRows1));

        pages.add(new TutorialPage(
                PageType.ITEMS,
                "ITEMS (2/2)",
                "More tools at your disposal.",
                itemRows2));

        List<EntryRow> envRows = new ArrayList<>();
        envRows.add(new EntryRow(
                "Puddles", "Hazard",
                new Color(100, 180, 255),
                "Floor decals found in Normal Mode.",
                "Stepping here creates a loud SPLASH!",
                "Nearby enemies will investigate the noise immediately."));
        envRows.add(new EntryRow(
                "Campfire", "Sanctuary",
                new Color(255, 140, 40),
                "A rare, warm light source.",
                "Resting (staying still) for 5 turns restores 1 HP.",
                "Restriction: Can only rest once every two floors."));

        pages.add(new TutorialPage(
                PageType.ENVIRONMENT,
                "ENVIRONMENT",
                "Learn to use your surroundings to survive.",
                envRows));

        List<EntryRow> enemyRows = new ArrayList<>();
        // -- ADD NEW ENEMIES BELOW THIS LINE --
        enemyRows.add(new EntryRow(
                "Patrol Guard", "",
                new Color(220, 100, 80),
                "Walks a fixed route back and forth.",
                "If it spots you it will chase aggressively.",
                "Loses sight and searches briefly before resuming patrol."));
        enemyRows.add(new EntryRow(
                "Hunter", "",
                new Color(200, 60, 60),
                "Actively hunts -- smarter pathfinding than the Patrol Guard.",
                "On higher floors it may place Sticky Traps in your path.",
                "Once alerted it is very persistent."));
        enemyRows.add(new EntryRow(
                "Sentry", "",
                new Color(240, 140, 40),
                "Stationary guard with a wide detection arc.",
                "When it spots you, it alerts all nearby enemies.",
                "BEWARE: It has a chance to leave its post and CHASE you!"));
        // -- END ENEMIES --

        pages.add(new TutorialPage(
                PageType.ENEMIES,
                "ENEMIES",
                "Standard threats found on every floor.",
                enemyRows));

        pages.add(new TutorialPage(
                PageType.HORROR,
                "HORROR MODE",
                "A more intense, psychological experience.",
                "Horror Mode is toggled with 'X' in the Main Menu.",
                "It introduces new mechanics to challenge your sanity:",
                " ",
                "* TOTAL DARKNESS: Lights may fail, forcing you to move blind.",
                "* UNRELIABLE TOOLS: Your lantern may flicker or fail in the dark.",
                "* TENSION CYCLE: High-tension pulses bring audio hallucinations.",
                "* THE LIMIT: You cannot linger. Something hunts you after Turn 40.",
                "* PURIFICATION: Use the Holy Cross [5] to return to Normal Mode."));

        List<EntryRow> horrorRows = new ArrayList<>();
        horrorRows.add(new EntryRow(
                "Shadow Figure", "",
                new Color(40, 40, 45),
                "Manifests only in total darkness.",
                "It watches from the edge of your vision.",
                "If it catches your gaze... it will vanish with a scream."));
        horrorRows.add(new EntryRow(
                "The Stalker", "",
                new Color(120, 20, 20),
                "A persistent, invincible predator.",
                "Spawns if you linger too long on a floor (Turn 40+).",
                "It knows where you are. It cannot be killed. ESCAPE."));
        horrorRows.add(new EntryRow(
                "Mirror Phantom", "",
                new Color(150, 150, 160),
                "A psychological manifestation of your guilt.",
                "Appears briefly in the distance, facing away.",
                "If you approach your own reflection... it will evade you."));

        pages.add(new TutorialPage(
                PageType.HORROR,
                "HORROR THREATS",
                "Strictly limited to HORROR MODE (Press 'X' in Menu).",
                horrorRows));

        return pages;
    }

    // -------------------------------------------------------------------------
    // Runtime state
    // -------------------------------------------------------------------------

    private boolean active = false; // NOT shown at startup -- opened via button
    private int pageIdx = 0;

    public boolean isActive() {
        return active;
    }

    public int currentPage() {
        return pageIdx;
    }

    public int totalPages() {
        return PAGES.size();
    }

    public TutorialPage currentPageData() {
        return PAGES.get(pageIdx);
    }

    public void nextPage() {
        if (pageIdx < PAGES.size() - 1)
            pageIdx++;
        else
            dismiss();
    }

    public void prevPage() {
        if (pageIdx > 0)
            pageIdx--;
    }

    public void dismiss() {
        active = false;
        pageIdx = 0;
    }

    /** Open the tutorial from page 1 (called when "How to Play" is clicked). */
    public void reset() {
        pageIdx = 0;
        active = true;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    public void draw(Graphics g, int panelW, int panelH) {
        if (!active)
            return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(new Color(0, 0, 0, 210));
        g2.fillRect(0, 0, panelW, panelH);

        int cardW = Math.min(680, panelW - 80);
        int cardH = Math.min(500, panelH - 80);
        int cardX = (panelW - cardW) / 2;
        int cardY = (panelH - cardH) / 2;

        g2.setColor(new Color(16, 13, 10, 245));
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 16, 16);
        g2.setColor(new Color(90, 58, 18, 200));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(cardX, cardY, cardW, cardH, 16, 16);
        g2.setColor(new Color(50, 33, 10, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(cardX + 5, cardY + 5, cardW - 10, cardH - 10, 12, 12);

        TutorialPage page = currentPageData();

        int dotY = cardY + 18;
        int dotR = 5;
        int dotGap = 16;
        int dotsW = PAGES.size() * dotGap - (dotGap - dotR * 2);
        int dotX = cardX + (cardW - dotsW) / 2;
        for (int i = 0; i < PAGES.size(); i++) {
            g2.setColor(i == pageIdx ? new Color(255, 230, 120) : new Color(80, 60, 30));
            g2.fillOval(dotX, dotY, dotR * 2, dotR * 2);
            dotX += dotGap;
        }

        int titleY = cardY + 52;
        g2.setFont(new Font("Serif", Font.BOLD, 28));
        FontMetrics tfm = g2.getFontMetrics();
        int tw = tfm.stringWidth(page.title);
        g2.setColor(new Color(255, 230, 120));
        g2.drawString(page.title, cardX + (cardW - tw) / 2, titleY);

        int subtitleY = titleY + 22;
        g2.setFont(new Font("SansSerif", Font.ITALIC, 13));
        FontMetrics sfm = g2.getFontMetrics();
        int sw = sfm.stringWidth(page.subtitle);
        g2.setColor(new Color(160, 140, 100));
        g2.drawString(page.subtitle, cardX + (cardW - sw) / 2, subtitleY);

        int divY = subtitleY + 10;
        g2.setColor(new Color(90, 58, 18, 160));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(cardX + 40, divY, cardX + cardW - 40, divY);

        int contentY = divY + 20;
        int contentMaxH = cardY + cardH - 70 - contentY;
        int contentX = cardX + 36;
        int contentW = cardW - 72;

        switch (page.type) {
            case INTRO:
            case CONTROLS:
                drawTextPage(g2, page, contentX, contentY, contentW, contentMaxH);
                break;
            case ITEMS:
            case ENEMIES:
                drawEntryPage(g2, page, contentX, contentY, contentW, contentMaxH);
                break;
            case HORROR:
                if (page.entries != null && !page.entries.isEmpty()) {
                    drawEntryPage(g2, page, contentX, contentY, contentW, contentMaxH);
                } else {
                    drawTextPage(g2, page, contentX, contentY, contentW, contentMaxH);
                }
                break;
        }

        int btnY = cardY + cardH - 48;
        int btnH = 32;
        int btnW = 110;

        if (pageIdx > 0) {
            drawNavButton(g2, cardX + 20, btnY, btnW, btnH, "< Back",
                    new Color(80, 60, 30, 200), new Color(160, 130, 60));
        }

        String nextLabel = (pageIdx == PAGES.size() - 1) ? "Start Game >" : "Next >";
        Color nextFill = (pageIdx == PAGES.size() - 1)
                ? new Color(60, 100, 50, 220)
                : new Color(60, 45, 15, 220);
        Color nextText = (pageIdx == PAGES.size() - 1)
                ? new Color(140, 220, 100)
                : new Color(220, 185, 80);
        drawNavButton(g2, cardX + cardW - btnW - 20, btnY, btnW, btnH, nextLabel, nextFill, nextText);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        String hint = pageIdx > 0 ? "< / > arrow keys  or  click buttons" : "> arrow key  or  click Next";
        FontMetrics hfm = g2.getFontMetrics();
        g2.setColor(new Color(100, 90, 70));
        g2.drawString(hint, cardX + (cardW - hfm.stringWidth(hint)) / 2, btnY + btnH + 14);

        g2.dispose();
    }

    private void drawTextPage(Graphics2D g2, TutorialPage page,
            int x, int y, int w, int maxH) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        FontMetrics fm = g2.getFontMetrics();
        int lineH = fm.getHeight() + 3;
        int cy = y;
        for (String line : page.body) {
            if (cy + lineH > y + maxH)
                break;
            if (line.isBlank()) {
                cy += lineH / 2;
                continue;
            }
            g2.setColor(new Color(210, 195, 160));
            g2.drawString(line, x, cy + fm.getAscent());
            cy += lineH;
        }
    }

    private void drawEntryPage(Graphics2D g2, TutorialPage page,
            int x, int y, int w, int maxH) {
        int cy = y;
        int swatchW = 6;
        int rowGap = 14;
        int descIndent = x + swatchW + 14;

        for (EntryRow row : page.entries) {
            if (cy > y + maxH - 20)
                break;

            g2.setColor(row.accent);
            g2.fillRect(x, cy, swatchW, 14 + row.lines.length * 17);

            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            FontMetrics bfm = g2.getFontMetrics();
            g2.setColor(new Color(230, 210, 160));
            g2.drawString(row.name, descIndent, cy + bfm.getAscent());

            if (!row.key.isEmpty()) {
                int nameW = bfm.stringWidth(row.name);
                g2.setFont(new Font("Monospaced", Font.BOLD, 12));
                FontMetrics kfm = g2.getFontMetrics();
                int keyW = kfm.stringWidth(row.key) + 8;
                int keyX = descIndent + nameW + 10;
                int keyY = cy;
                int keyH = 16;
                g2.setColor(new Color(70, 55, 20, 200));
                g2.fillRoundRect(keyX, keyY, keyW, keyH, 6, 6);
                g2.setColor(new Color(90, 65, 18));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(keyX, keyY, keyW, keyH, 6, 6);
                g2.setColor(new Color(220, 185, 80));
                g2.drawString(row.key, keyX + 4, keyY + kfm.getAscent() + 1);
            }

            cy += bfm.getHeight() + 2;

            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            FontMetrics dfm = g2.getFontMetrics();
            for (String line : row.lines) {
                if (cy > y + maxH - 10)
                    break;
                g2.setColor(new Color(170, 158, 130));
                g2.drawString(line, descIndent, cy + dfm.getAscent());
                cy += dfm.getHeight() + 1;
            }

            cy += rowGap;
        }
    }

    private void drawNavButton(Graphics2D g2, int x, int y, int w, int h,
            String label, Color fill, Color textColor) {
        g2.setColor(fill);
        g2.fillRoundRect(x, y, w, h, 10, 10);
        g2.setColor(textColor.darker());
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 10, 10);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int lw = fm.stringWidth(label);
        g2.setColor(textColor);
        g2.drawString(label, x + (w - lw) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
    }

    // -------------------------------------------------------------------------
    // Mouse click handling
    // -------------------------------------------------------------------------

    /**
     * Returns true if tutorial consumed the click, false if "Start Game" was
     * pressed.
     */
    public boolean handleClick(int mouseX, int mouseY, int panelW, int panelH) {
        if (!active)
            return false;

        int cardW = Math.min(680, panelW - 80);
        int cardH = Math.min(500, panelH - 80);
        int cardX = (panelW - cardW) / 2;
        int cardY = (panelH - cardH) / 2;
        int btnY = cardY + cardH - 48;
        int btnH = 32;
        int btnW = 110;

        // Next / Start button
        int nextX = cardX + cardW - btnW - 20;
        if (mouseX >= nextX && mouseX <= nextX + btnW
                && mouseY >= btnY && mouseY <= btnY + btnH) {
            if (pageIdx == PAGES.size() - 1) {
                dismiss();
                return false; // signal: start the game
            }
            nextPage();
            return true;
        }

        // Prev button
        if (pageIdx > 0) {
            int prevX = cardX + 20;
            if (mouseX >= prevX && mouseX <= prevX + btnW
                    && mouseY >= btnY && mouseY <= btnY + btnH) {
                prevPage();
                return true;
            }
        }

        return true; // consume all other clicks
    }
}