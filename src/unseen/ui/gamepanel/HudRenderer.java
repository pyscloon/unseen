package unseen.ui.gamepanel;

import unseen.items.Flare;
import unseen.items.GrapplingHook;
import unseen.items.NoiseMaker;
import unseen.items.Shuriken;
import unseen.items.SmokeBomb;
import unseen.entities.Player;
import unseen.ui.GamePanel;
import unseen.utils.AssetLoader;

import java.awt.*;
import java.util.List;
import java.util.Random;

class HudRenderer {
    private final GamePanel panel;
    private final LevelManager levelManager;

    HudRenderer(GamePanel panel, LevelManager levelManager) {
        this.panel = panel;
        this.levelManager = levelManager;
    }

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


    void drawToasts(Graphics g) {
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


    void drawHealthBar(Graphics g) {
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


    void drawInventory(Graphics g) {
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


    void drawLoreNote(Graphics g) {
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


}
