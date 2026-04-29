package unseen.ui.gamepanel;

import unseen.utils.AssetLoader;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
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
        public final Image icon;
        public final String[] lines;

        public EntryRow(String name, String key, Color accent, Image icon, String... lines) {
            this.name = name;
            this.key = key;
            this.accent = accent;
            this.icon = icon;
            this.lines = lines;
        }
    }

    private static final class TutorialLayout {
        final int cardW;
        final int cardH;
        final int cardX;
        final int cardY;
        final int titleY;
        final int subtitleY;
        final int dividerY;
        final int contentX;
        final int contentY;
        final int contentW;
        final int contentMaxH;
        final Rectangle prevButton;
        final Rectangle nextButton;
        final List<Ellipse2D.Float> dots;

        TutorialLayout(int cardW, int cardH, int cardX, int cardY, int titleY, int subtitleY,
                int dividerY, int contentX, int contentY, int contentW, int contentMaxH,
                Rectangle prevButton, Rectangle nextButton, List<Ellipse2D.Float> dots) {
            this.cardW = cardW;
            this.cardH = cardH;
            this.cardX = cardX;
            this.cardY = cardY;
            this.titleY = titleY;
            this.subtitleY = subtitleY;
            this.dividerY = dividerY;
            this.contentX = contentX;
            this.contentY = contentY;
            this.contentW = contentW;
            this.contentMaxH = contentMaxH;
            this.prevButton = prevButton;
            this.nextButton = nextButton;
            this.dots = dots;
        }
    }

    private static final List<TutorialPage> PAGES = buildPages();

    private static List<TutorialPage> buildPages() {
        List<TutorialPage> pages = new ArrayList<>();
        AssetLoader assets = AssetLoader.get();

        pages.add(new TutorialPage(
                PageType.INTRO,
                "UNSEEN",
                "A stealth roguelike. Stay in the shadows. Survive the floors.",
                "Beneath Room 205 in the Engineering Building, under an old",
                "software engineering classroom, a hidden dungeon was found.",
                "A spreading virus twisted the depths, and monsters began",
                "appearing in the dark like broken code made flesh.",
                " ",
                "The first heroes were Lon, Ron, and Dom.",
                "They challenged the dungeon before you and left behind",
                "the ancient relics that now appear as the items you find.",
                "Now they are either missing, or dead.",
                " ",
                "You are the one meant to succeed them.",
                "One day, while searching for adventure, you discovered",
                "the dungeon in Room 205 and chose to challenge it yourself.",
                " ",
                "GOAL: Reach the EXIT tile on each floor without being caught.",
                "If an enemy steps onto your tile, the run is over.",
                " ",
                "INSTRUCTIONS: Use items, stealth, and careful movement",
                "to slip past every threat.",
                " ",
                "WATCH YOUR STEP: Puddles can alert nearby enemies",
                "if you step on them. (Normal Mode)"));

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
                new Color(255, 210, 60), assets.noiseMaker,
                "Throw it to any visible floor tile.",
                "Enemies within range will investigate the sound,",
                "drawing them away from their patrol route."));
        itemRows.add(new EntryRow(
                "Smoke Bomb", "[2]",
                new Color(160, 190, 220), assets.smokeBomb,
                "Instantly detonates on your tile.",
                "Creates a smoke cloud that blocks enemy line-of-sight",
                "for several turns. Great for emergency cover."));
        itemRows.add(new EntryRow(
                "Flare / Lantern", "[3]",
                new Color(255, 240, 100), assets.lantern,
                "Throw it to any visible floor tile.",
                "Illuminates a wide radius for many turns --",
                "useful for scouting ahead or confusing sentries."));
        itemRows.add(new EntryRow(
                "Shuriken", "[4]",
                new Color(180, 220, 255), assets.shuriken,
                "Press 4 to enter aim mode. Use WASD to set direction.",
                "Press Space or Enter to throw. Travels up to 5 tiles",
                "in a straight line and silently eliminates the first enemy hit.",
                "Wall stops the shuriken. Only one throw per shuriken."));
        itemRows.add(new EntryRow(
                "Grappling Hook", "[5]",
                new Color(120, 200, 255), assets.grapplingHook,
                "Fire a hook at any wall tile within 6 tiles (non-straight paths OK).",
                "You'll zip to the nearest floor tile next to the wall.",
                "Can bypass enemies, but zipping through them deals damage!"));
        itemRows.add(new EntryRow(
                "Holy Cross", "[6]",
                new Color(255, 255, 180), assets.cross,
                "A sacred artifact that only functions in HORROR MODE.",
                "Instantly purifies the floor: banishes the Stalker,",
                "cleanses blood, and reverts atmosphere to Normal Mode.",
                "Extremely rare. Use it when the darkness becomes too much."));
        // -- END ITEMS --

        List<EntryRow> itemRows1 = new ArrayList<>(itemRows.subList(0, 2));
        List<EntryRow> itemRows2 = new ArrayList<>(itemRows.subList(2, 4));
        List<EntryRow> itemRows3 = new ArrayList<>(itemRows.subList(4, itemRows.size()));

        pages.add(new TutorialPage(
                PageType.ITEMS,
                "ITEMS (1/3)",
                "Each item is consumed on use. Pick up more on each floor.",
                itemRows1));

        pages.add(new TutorialPage(
                PageType.ITEMS,
                "ITEMS (2/3)",
                "More tools at your disposal.",
                itemRows2));

        pages.add(new TutorialPage(
                PageType.ITEMS,
                "ITEMS (3/3)",
                "Mobility and purification tools.",
                itemRows3));

        List<EntryRow> envRows = new ArrayList<>();
        envRows.add(new EntryRow(
                "Puddles", "Hazard",
                new Color(100, 180, 255), assets.puddle,
                "Floor decals found in Normal Mode.",
                "Stepping here creates a loud SPLASH!",
                "Nearby enemies will investigate the noise immediately."));
        envRows.add(new EntryRow(
                "Campfire", "Sanctuary",
                new Color(255, 140, 40), assets.campfire,
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
                new Color(220, 100, 80), assets.patrol,
                "Walks a fixed route back and forth.",
                "If it spots you it will chase aggressively.",
                "Loses sight and searches briefly before resuming patrol."));
        enemyRows.add(new EntryRow(
                "Hunter", "",
                new Color(200, 60, 60), assets.enemyBase,
                "Actively hunts -- smarter pathfinding than the Patrol Guard.",
                "On higher floors it may place Sticky Traps in your path.",
                "Once alerted it is very persistent."));
        enemyRows.add(new EntryRow(
                "Sentry", "",
                new Color(240, 140, 40), assets.sentry,
                "Stationary guard with a wide detection arc.",
                "When it spots you, it alerts all nearby enemies.",
                "BEWARE: It has a chance to leave its post and CHASE you!"));
        enemyRows.add(new EntryRow(
                "Crawler", "",
                new Color(140, 80, 20),
                assets.crawler != null ? assets.crawler : assets.patrol,
                "Completely blind — ignores light, darkness, and smoke.",
                "Detects you by proximity (≤ 3 tiles) or sound. When triggered,",
                "it charges at 2 steps per turn — twice as fast as you.",
                "Smoke bombs are useless. Noise Maker lures it away.",
                "A Shuriken is a clean one-shot. Stay ≥ 4 tiles away to be safe.",
                "First appears on Floor 3."
        ));


        // -- END ENEMIES --

        List<EntryRow> enemyRows1 = new ArrayList<>(enemyRows.subList(0, 2));
        List<EntryRow> enemyRows2 = new ArrayList<>(enemyRows.subList(2, enemyRows.size()));

        pages.add(new TutorialPage(
                PageType.ENEMIES,
                "ENEMIES (1/2)",
                "Standard threats found on every floor.",
                enemyRows1));

        pages.add(new TutorialPage(
                PageType.ENEMIES,
                "ENEMIES (2/2)",
                "Advanced threats that demand different counterplay.",
                enemyRows2));
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
                "* PURIFICATION: Use the Holy Cross [6] to return to Normal Mode."));

        List<EntryRow> horrorRows = new ArrayList<>();
        horrorRows.add(new EntryRow(
                "Shadow Figure", "",
                new Color(40, 40, 45), null,
                "Manifests only in total darkness.",
                "It watches from the edge of your vision.",
                "If it catches your gaze... it will vanish with a scream."));
        horrorRows.add(new EntryRow(
                "The Stalker", "",
                new Color(120, 20, 20), null,
                "A persistent, invincible predator.",
                "Spawns if you linger too long on a floor (Turn 40+).",
                "It knows where you are. It cannot be killed. ESCAPE."));
        horrorRows.add(new EntryRow(
                "Mirror Phantom", "",
                new Color(150, 150, 160), null,
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
        long now = System.currentTimeMillis();
        TutorialLayout layout = buildLayout(panelW, panelH);
        TutorialPage page = currentPageData();
        g2.setColor(new Color(0, 0, 0, 214));
        g2.fillRect(0, 0, panelW, panelH);
        drawMist(g2, panelW, panelH, now);

        RoundRectangle2D.Float card = new RoundRectangle2D.Float(
                layout.cardX, layout.cardY, layout.cardW, layout.cardH, 34, 34);
        g2.setPaint(new GradientPaint(
                layout.cardX, layout.cardY, new Color(22, 18, 17, 212),
                layout.cardX, layout.cardY + layout.cardH, new Color(8, 8, 10, 238)));
        g2.fill(card);
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fillRoundRect(layout.cardX + 18, layout.cardY + 18, layout.cardW - 36, 52, 24, 24);
        g2.setColor(new Color(205, 154, 72, 210));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(card);
        g2.setColor(new Color(110, 74, 28, 150));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(layout.cardX + 7, layout.cardY + 7, layout.cardW - 14, layout.cardH - 14, 28, 28);

        for (int i = 0; i < layout.dots.size(); i++) {
            Ellipse2D.Float dot = layout.dots.get(i);
            if (i == pageIdx) {
                float pulse = 0.65f + 0.35f * (float) Math.sin(now / 240.0);
                g2.setColor(new Color(255, 168, 58, 28 + (int) (42 * pulse)));
                g2.fillOval((int) dot.x - 6, (int) dot.y - 6, (int) dot.width + 12, (int) dot.height + 12);
                g2.setColor(new Color(255, 228, 134));
                g2.fill(dot);
                g2.setColor(new Color(255, 247, 212, 170));
                g2.draw(dot);
            } else {
                g2.setColor(new Color(82, 64, 34, 185));
                g2.fill(dot);
            }
        }

        g2.setFont(new Font("Serif", Font.BOLD, 28));
        FontMetrics tfm = g2.getFontMetrics();
        int tw = tfm.stringWidth(page.title);
        g2.setColor(new Color(0, 0, 0, 145));
        g2.drawString(page.title, layout.cardX + (layout.cardW - tw) / 2 + 2, layout.titleY + 2);
        g2.setColor(new Color(255, 230, 120));
        g2.drawString(page.title, layout.cardX + (layout.cardW - tw) / 2, layout.titleY);

        g2.setFont(new Font("SansSerif", Font.ITALIC, 13));
        FontMetrics sfm = g2.getFontMetrics();
        int sw = sfm.stringWidth(page.subtitle);
        g2.setColor(new Color(160, 140, 100));
        g2.drawString(page.subtitle, layout.cardX + (layout.cardW - sw) / 2, layout.subtitleY);

        g2.setColor(new Color(90, 58, 18, 160));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(layout.cardX + 40, layout.dividerY, layout.cardX + layout.cardW - 40, layout.dividerY);

        switch (page.type) {
            case INTRO:
            case CONTROLS:
                drawTextPage(g2, page, layout.contentX, layout.contentY, layout.contentW, layout.contentMaxH);
                break;
            case ITEMS:
            case ENVIRONMENT:
            case ENEMIES:
                drawEntryPage(g2, page, layout.contentX, layout.contentY, layout.contentW, layout.contentMaxH);
                break;
            case HORROR:
                if (page.entries != null && !page.entries.isEmpty()) {
                    drawEntryPage(g2, page, layout.contentX, layout.contentY, layout.contentW, layout.contentMaxH);
                } else {
                    drawTextPage(g2, page, layout.contentX, layout.contentY, layout.contentW, layout.contentMaxH);
                }
                break;
        }

        if (pageIdx > 0) {
            drawNavButton(g2, layout.prevButton, "< Back",
                    new Color(72, 54, 34, 210), new Color(190, 154, 84));
        }

        String nextLabel = (pageIdx == PAGES.size() - 1) ? "Start Game" : "Next >";
        Color nextFill = (pageIdx == PAGES.size() - 1)
                ? new Color(62, 102, 60, 226)
                : new Color(92, 58, 18, 220);
        Color nextText = (pageIdx == PAGES.size() - 1)
                ? new Color(172, 236, 126)
                : new Color(238, 198, 96);
        drawNavButton(g2, layout.nextButton, nextLabel, nextFill, nextText);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        String hint = pageIdx > 0 ? "< / > arrow keys  or  click buttons" : "> arrow key  or  click Next";
        FontMetrics hfm = g2.getFontMetrics();
        g2.setColor(new Color(100, 90, 70));
        g2.drawString(hint, layout.cardX + (layout.cardW - hfm.stringWidth(hint)) / 2,
                layout.nextButton.y + layout.nextButton.height + 16);

        g2.dispose();
    }

    private void drawTextPage(Graphics2D g2, TutorialPage page,
            int x, int y, int w, int maxH) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        FontMetrics fm = g2.getFontMetrics();
        int lineH = fm.getHeight() + 6;
        int cy = y;
        for (String line : page.body) {
            if (cy + lineH > y + maxH)
                break;
            if (line.isBlank()) {
                cy += lineH / 2 + 2;
                continue;
            }
            if (page.type == PageType.CONTROLS && line.contains("--")) {
                String[] parts = line.split("--", 2);
                String left = parts[0].trim();
                String right = parts[1].trim();
                int badgeW = Math.max(44, g2.getFontMetrics(new Font("DialogInput", Font.BOLD, 12)).stringWidth(left) + 18);
                drawKeyBadge(g2, left, x, cy - 1, badgeW, 20, new Color(198, 158, 82), new Color(24, 20, 18, 220));
                g2.setColor(new Color(210, 195, 160));
                g2.drawString(right, x + badgeW + 14, cy + fm.getAscent());
            } else if (line.startsWith("*")) {
                g2.setColor(new Color(255, 182, 92));
                g2.drawString(line, x, cy + fm.getAscent());
            } else {
                g2.setColor(new Color(210, 195, 160));
                g2.drawString(line, x, cy + fm.getAscent());
            }
            cy += lineH;
        }
    }

    private void drawEntryPage(Graphics2D g2, TutorialPage page,
            int x, int y, int w, int maxH) {
        int cy = y;
        int rowGap = 16;
        Font titleFont = new Font("SansSerif", Font.BOLD, 15);
        Font bodyFont = new Font("SansSerif", Font.PLAIN, 13);
        FontMetrics titleFm = g2.getFontMetrics(titleFont);
        FontMetrics bodyFm = g2.getFontMetrics(bodyFont);

        for (EntryRow row : page.entries) {
            boolean hasIcon = row.icon != null;
            int topPad = 18;
            int bottomPad = 18;
            int iconBox = 46;
            int iconStartX = x + 28;
            int iconBlockH = hasIcon ? (topPad + iconBox) : topPad;
            int descIndent = hasIcon ? (iconStartX + iconBox + 18) : (x + 30);
            int titleBlockH = topPad + titleFm.getHeight();
            int bodyStartY = cy + titleBlockH + 10;
            int bodyBlockH = row.lines.length * bodyFm.getHeight() + Math.max(0, row.lines.length - 1) * 3;
            int textBlockH = titleBlockH + 10 + bodyBlockH;
            int rowH = Math.max(90, Math.max(iconBlockH, textBlockH) + bottomPad);
            if (cy + rowH > y + maxH)
                break;

            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRoundRect(x + 4, cy + 5, w, rowH, 24, 24);
            g2.setColor(new Color(20, 18, 18, 170));
            g2.fillRoundRect(x, cy, w, rowH, 24, 24);
            g2.setColor(new Color(row.accent.getRed(), row.accent.getGreen(), row.accent.getBlue(), 178));
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawRoundRect(x, cy, w, rowH, 24, 24);
            g2.setColor(new Color(255, 255, 255, 14));
            g2.fillRoundRect(x + 12, cy + 8, w - 24, 18, 18, 18);

            g2.setColor(row.accent);
            g2.fillRoundRect(x + 14, cy + 14, 7, rowH - 28, 6, 6);

            if (hasIcon) {
                int iconY = cy + topPad;
                g2.setColor(new Color(12, 10, 9, 190));
                g2.fillRoundRect(iconStartX, iconY, iconBox, iconBox, 16, 16);
                g2.setColor(new Color(row.accent.getRed(), row.accent.getGreen(), row.accent.getBlue(), 160));
                g2.drawRoundRect(iconStartX, iconY, iconBox, iconBox, 16, 16);
                drawEntryIcon(g2, row, iconStartX, iconY, iconBox);
            }

            g2.setFont(titleFont);
            g2.setColor(new Color(236, 218, 180));
            g2.drawString(row.name, descIndent, cy + topPad + titleFm.getAscent());

            if (!row.key.isEmpty()) {
                int nameW = titleFm.stringWidth(row.name);
                int keyX = Math.min(x + w - 78, descIndent + nameW + 14);
                drawKeyBadge(g2, row.key, keyX, cy + topPad - 1, 58, 22,
                        new Color(232, 190, 92), new Color(24, 20, 18, 220));
            }

            g2.setFont(bodyFont);
            int lineY = bodyStartY;
            for (String line : row.lines) {
                g2.setColor(new Color(170, 158, 130));
                g2.drawString(line, descIndent, lineY + bodyFm.getAscent());
                lineY += bodyFm.getHeight() + 3;
            }

            cy += rowH + rowGap;
        }
    }

    private void drawNavButton(Graphics2D g2, Rectangle bounds, String label, Color fill, Color textColor) {
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(bounds.x + 4, bounds.y + 5, bounds.width, bounds.height, 22, 22);
        g2.setPaint(new GradientPaint(
                bounds.x, bounds.y, brighten(fill, 0.08f),
                bounds.x, bounds.y + bounds.height, darken(fill, 0.14f)));
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 22, 22);
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fillRoundRect(bounds.x + 12, bounds.y + 6, bounds.width - 24, 10, 14, 14);
        g2.setColor(brighten(textColor, 0.08f));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 22, 22);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(textColor);
        g2.drawString(label, bounds.x + (bounds.width - fm.stringWidth(label)) / 2,
                bounds.y + (bounds.height - fm.getHeight()) / 2 + fm.getAscent());
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

        TutorialLayout layout = buildLayout(panelW, panelH);

        for (int i = 0; i < layout.dots.size(); i++) {
            if (layout.dots.get(i).contains(mouseX, mouseY)) {
                unseen.utils.SoundManager.get().play("ui_click", 0.75f);
                pageIdx = i;
                return true;
            }
        }

        if (layout.nextButton.contains(mouseX, mouseY)) {
            unseen.utils.SoundManager.get().play("ui_click", 0.75f);
            if (pageIdx == PAGES.size() - 1) {
                dismiss();
                return false;
            }
            nextPage();
            return true;
        }

        if (pageIdx > 0 && layout.prevButton.contains(mouseX, mouseY)) {
            unseen.utils.SoundManager.get().play("ui_click", 0.75f);
            prevPage();
            return true;
        }

        return true;
    }

    private TutorialLayout buildLayout(int panelW, int panelH) {
        int cardW = Math.min(760, panelW - 72);
        int cardH = Math.min(560, panelH - 64);
        int cardX = (panelW - cardW) / 2;
        int cardY = (panelH - cardH) / 2;
        int titleY = cardY + 58;
        int subtitleY = titleY + 24;
        int dividerY = subtitleY + 12;
        int contentX = cardX + 34;
        int contentY = dividerY + 24;
        int buttonH = 40;
        int buttonW = 138;
        int buttonY = cardY + cardH - 58;
        int contentMaxH = buttonY - 22 - contentY;
        Rectangle prevButton = new Rectangle(cardX + 24, buttonY, buttonW, buttonH);
        Rectangle nextButton = new Rectangle(cardX + cardW - buttonW - 24, buttonY, buttonW, buttonH);

        int dotR = 6;
        int dotGap = 19;
        int dotsW = PAGES.size() * (dotR * 2) + (PAGES.size() - 1) * dotGap;
        int dotX = cardX + (cardW - dotsW) / 2;
        int dotY = cardY + 20;
        List<Ellipse2D.Float> dots = new ArrayList<>();
        for (int i = 0; i < PAGES.size(); i++) {
            dots.add(new Ellipse2D.Float(dotX, dotY, dotR * 2, dotR * 2));
            dotX += dotR * 2 + dotGap;
        }

        return new TutorialLayout(cardW, cardH, cardX, cardY, titleY, subtitleY, dividerY,
                contentX, contentY, cardW - 68, contentMaxH, prevButton, nextButton, dots);
    }

    private void drawMist(Graphics2D g2, int panelW, int panelH, long now) {
        Graphics2D mist = (Graphics2D) g2.create();
        mist.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int layer = 0; layer < 3; layer++) {
            mist.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.035f + layer * 0.012f));
            mist.setColor(new Color(210 - layer * 14, 206 - layer * 16, 196 - layer * 18));
            for (int i = 0; i < 4 + layer; i++) {
                double drift = ((now * (0.014 + layer * 0.004)) + i * 210.0) % (panelW + 280.0) - 140.0;
                double wave = Math.sin(now / (1900.0 + layer * 320.0) + i * 0.9) * (18 + layer * 10);
                int cloudW = 180 + layer * 80;
                int cloudH = 54 + layer * 16;
                mist.fillRoundRect((int) drift, (int) (panelH * (0.24 + layer * 0.18) + wave),
                        cloudW, cloudH, cloudH, cloudH);
            }
        }
        mist.dispose();
    }

    private void drawEntryIcon(Graphics2D g2, EntryRow row, int x, int y, int size) {
        if (row.icon != null) {
            g2.drawImage(row.icon, x + 5, y + 5, size - 10, size - 10, null);
        }
    }

    private void drawKeyBadge(Graphics2D g2, String text, int x, int y, int w, int h, Color border, Color fill) {
        g2.setColor(fill);
        g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fillRoundRect(x + 5, y + 4, w - 10, 7, 8, 8);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w, h, 12, 12);
        g2.setFont(new Font("DialogInput", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x + (w - fm.stringWidth(text)) / 2,
                y + (h - fm.getHeight()) / 2 + fm.getAscent());
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
}
