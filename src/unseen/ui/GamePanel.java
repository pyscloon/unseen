package unseen.ui;

import javax.swing.JPanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import unseen.map.*;
import unseen.entities.*;
import unseen.utils.Constants;
import unseen.game.GameState;
import unseen.ai.AStar;
import unseen.ai.LineOfSight;
import unseen.game.TurnManager;
import unseen.game.SmokeSpawner;
import unseen.items.*;
import unseen.game.Smoke;
import unseen.utils.AssetLoader;

public class GamePanel extends JPanel implements Runnable, SmokeSpawner {

    public void pauseGame() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
            repaint();
        }
    }

    public void resumeGame() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
            requestFocusInWindow();
            repaint();
        }
    }

    // Restart the game after death
    public void restartGame() {
        setupGame();
        setGameState(GameState.PLAYING);
        requestFocusInWindow();
        if (backgroundClip != null) {
            backgroundClip.setFramePosition(0);
            backgroundClip.loop(javax.sound.sampled.Clip.LOOP_CONTINUOUSLY);
        }
        repaint();
    }

    private javax.sound.sampled.Clip backgroundClip;

    private Thread gameThread;
    private GameState state = GameState.PLAYING;

    private int floorNumber = 1;

    private Map map;
    private Player player;
    private List<Enemy> enemies;
    private boolean[][] visible;
    private List<Smoke> smokes = new ArrayList<>();
    private List<FlashEffect> noiseFlashes = new ArrayList<>();
    private List<unseen.game.ActiveFlare> flares = new ArrayList<>();

    // Noise-maker targeting mode
    private boolean targetingNoiseMaker = false;
    // Flare targeting mode
    private boolean targetingFlare = false;
    private int mouseGridX = 0;
    private int mouseGridY = 0;

    /** Ripple/pulse effect drawn at the NoiseMaker decoy tile for a few turns. */
    private static class FlashEffect {
        final int x, y;
        int countdown;

        FlashEffect(int x, int y, int countdown) {
            this.x = x;
            this.y = y;
            this.countdown = countdown;
        }
    }

    // All sprites are loaded once via AssetLoader singleton

    public GamePanel() {
        this.setPreferredSize(
                new Dimension(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(new InputHandler(this));
        this.setFocusable(true);

        this.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                mouseGridX = e.getX() / Constants.TILE_SIZE;
                mouseGridY = e.getY() / Constants.TILE_SIZE;
                if (targetingNoiseMaker || targetingFlare)
                    repaint();
            }
        });
        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (targetingNoiseMaker) {
                    confirmNoiseTarget(mouseGridX, mouseGridY);
                } else if (targetingFlare) {
                    confirmFlareTarget(mouseGridX, mouseGridY);
                }
            }
        });

        setupGame();
        loadAndPlayBackgroundSound();
    }

    private void loadAndPlayBackgroundSound() {
        try {
            java.net.URL soundURL = getClass().getClassLoader().getResource("assets/background.wav");
            if (soundURL != null) {
                javax.sound.sampled.AudioInputStream audioIn = javax.sound.sampled.AudioSystem
                        .getAudioInputStream(soundURL);
                backgroundClip = javax.sound.sampled.AudioSystem.getClip();
                backgroundClip.open(audioIn);
                backgroundClip.loop(javax.sound.sampled.Clip.LOOP_CONTINUOUSLY);
            }
        } catch (Exception e) {
            backgroundClip = null;
        }
    }

    /**
     * Generates a new map and places enemies randomly.
     * Does NOT touch the player or call updateVisibility — caller must do that.
     */
    private void buildFloor() {
        map = MapGenerator.generate(); // also updates Constants.START_X/Y
        ExitPlacer.placeExit(map);
        visible = new boolean[Constants.GRID_HEIGHT][Constants.GRID_WIDTH];
        smokes.clear();
        flares.clear();
        enemies = new ArrayList<>();

        Random rand = new Random();
        AStar pathfinder = new AStar();
        int minDist = 6;
        List<int[]> placed = new ArrayList<>();
        // Scale enemy count with floor: floor 1 = 3, +1 per floor, cap at 8
        int extraEnemies = Math.min(floorNumber - 1, 5);
        List<String> typeList = new java.util.ArrayList<>(java.util.Arrays.asList("patrol", "hunter", "sentry"));
        for (int i = 0; i < extraEnemies; i++) {
            typeList.add(i % 2 == 0 ? "patrol" : "hunter");
        }
        for (String type : typeList) {
            int ex = Constants.START_X, ey = Constants.START_Y;
            for (int attempt = 0; attempt < 500; attempt++) {
                int cx = 1 + rand.nextInt(Constants.GRID_WIDTH - 2);
                int cy = 1 + rand.nextInt(Constants.GRID_HEIGHT - 2);
                int dist = Math.abs(cx - Constants.START_X) + Math.abs(cy - Constants.START_Y);
                if (map.getTile(cx, cy) == unseen.map.Tile.FLOOR && dist >= minDist) {
                    boolean overlap = placed.stream().anyMatch(p -> p[0] == cx && p[1] == cy);
                    if (!overlap) {
                        ex = cx;
                        ey = cy;
                        placed.add(new int[] { cx, cy });
                        break;
                    }
                }
            }
            switch (type) {
                case "patrol":
                    enemies.add(new PatrolEnemy(ex, ey, pathfinder));
                    break;
                case "hunter":
                    enemies.add(new HunterEnemy(ex, ey, pathfinder));
                    break;
                case "sentry":
                    enemies.add(new SentryEnemy(ex, ey, pathfinder));
                    break;
            }
        }
    }

    private void setupGame() {
        floorNumber = 1;
        buildFloor();
        player = new Player(Constants.START_X, Constants.START_Y);
        player.addItem(new NoiseMaker());
        player.addItem(new SmokeBomb());
        player.addItem(new Flare());
        player.setSmokeSpawner(this);
        updateVisibility();
    }

    /**
     * Advance to the next floor: increment counter, regenerate map, reposition
     * player.
     */
    public void nextFloor() {
        floorNumber++;
        buildFloor();
        player.setPosition(Constants.START_X, Constants.START_Y);
        // Refill items for the new floor
        player.getInventory().clear();
        player.addItem(new NoiseMaker());
        player.addItem(new SmokeBomb());
        player.addItem(new Flare());
        updateVisibility();
        setGameState(GameState.PLAYING);
        requestFocusInWindow();
        repaint();
    }

    private void updateVisibility() {

        // Reset visibility
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                visible[y][x] = false;
            }
        }

        int px = player.getX();
        int py = player.getY();

        // Player vision (range 6)
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {

                if (LineOfSight.hasLineOfSight(map, px, py, x, y, 6, smokes)) {
                    visible[y][x] = true;
                }
            }
        }

        // Torch illumination — use LOS from each torch so walls block torch light
        final int TORCH_RANGE = 3;
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {

                if (map.getTile(x, y) == Tile.TORCH) {

                    for (int ty2 = 0; ty2 < Constants.GRID_HEIGHT; ty2++) {
                        for (int tx2 = 0; tx2 < Constants.GRID_WIDTH; tx2++) {

                            if (LineOfSight.hasLineOfSight(map, x, y, tx2, ty2, TORCH_RANGE)) {
                                visible[ty2][tx2] = true;
                            }
                        }
                    }
                }
            }
        }

        // Active flares
        for (unseen.game.ActiveFlare flare : flares) {
            int cx = flare.getX();
            int cy = flare.getY();
            int fr = flare.getRadius();
            for (int ty2 = 0; ty2 < Constants.GRID_HEIGHT; ty2++) {
                for (int tx2 = 0; tx2 < Constants.GRID_WIDTH; tx2++) {
                    if (LineOfSight.hasLineOfSight(map, cx, cy, tx2, ty2, fr)) {
                        visible[ty2][tx2] = true;
                    }
                }
            }
        }
    }

    public void updateSmoke() {

        List<Smoke> expired = new ArrayList<>();

        for (Smoke smoke : smokes) {
            smoke.decrease();
            if (smoke.isExpired()) {
                expired.add(smoke);
            }
        }

        smokes.removeAll(expired);

        // Decrement noise-flash countdowns, remove expired flashes
        List<FlashEffect> expiredFlashes = new ArrayList<>();
        for (FlashEffect f : noiseFlashes) {
            f.countdown--;
            if (f.countdown <= 0)
                expiredFlashes.add(f);
        }
        noiseFlashes.removeAll(expiredFlashes);

        // Update flares
        List<unseen.game.ActiveFlare> expiredFlares = new ArrayList<>();
        for (unseen.game.ActiveFlare flare : flares) {
            flare.decrease();
            if (flare.isExpired())
                expiredFlares.add(flare);
        }
        flares.removeAll(expiredFlares);

        // Always refresh FOV after a turn so rendering reflects the new state
        updateVisibility();
    }

    @Override
    public void spawnSmoke(int x, int y) {
        smokes.add(new Smoke(x, y, 2, 5)); // radius 2, lasts 5 turns
    }

    @Override
    public void spawnFlare(int x, int y) {
        flares.add(new unseen.game.ActiveFlare(x, y, 5, 10)); // radius 5, lasts 10 turns
    }

    /** Register a NoiseMaker ripple effect at the given tile for 4 turns. */
    public void addNoiseFlash(int x, int y) {
        noiseFlashes.add(new FlashEffect(x, y, 4));
    }

    /**
     * Enter noise-maker targeting mode — the next mouse click will place the decoy.
     */
    public void enterNoiseMakerTargeting() {
        targetingNoiseMaker = true;
        repaint();
    }

    /** Cancel targeting mode without using the item. */
    public void cancelTargeting() {
        targetingNoiseMaker = false;
        targetingFlare = false;
        repaint();
    }

    public boolean isTargetingNoiseMaker() {
        return targetingNoiseMaker;
    }

    public void enterFlareTargeting() {
        targetingFlare = true;
        repaint();
    }

    public boolean isTargetingFlare() {
        return targetingFlare;
    }

    /** Called when the player clicks a tile while in targeting mode. */
    private void confirmNoiseTarget(int gx, int gy) {
        // Must be within bounds and on a passable tile
        if (gx < 0 || gy < 0 || gx >= Constants.GRID_WIDTH || gy >= Constants.GRID_HEIGHT)
            return;
        if (!map.isPassable(gx, gy))
            return;

        // Find the NoiseMaker in inventory
        java.util.List<unseen.items.Item> inv = player.getInventory();
        int idx = -1;
        unseen.items.NoiseMaker nm = null;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i) instanceof unseen.items.NoiseMaker) {
                idx = i;
                nm = (unseen.items.NoiseMaker) inv.get(i);
                break;
            }
        }
        if (nm == null) {
            targetingNoiseMaker = false;
            repaint();
            return;
        }

        // Fire at the chosen tile
        nm.useAt(player, map, enemies, gx, gy);
        inv.remove(idx);
        addNoiseFlash(gx, gy);

        targetingNoiseMaker = false;

        GameState result = TurnManager.processTurn(player, enemies, map, smokes);
        setGameState(result);
        updateSmoke();
        requestFocusInWindow();
        repaint();
    }

    private void confirmFlareTarget(int gx, int gy) {
        // Must be within bounds and on a passable tile
        if (gx < 0 || gy < 0 || gx >= Constants.GRID_WIDTH || gy >= Constants.GRID_HEIGHT)
            return;
        if (!map.isPassable(gx, gy))
            return;

        // Find the Flare in inventory
        java.util.List<unseen.items.Item> inv = player.getInventory();
        int idx = -1;
        unseen.items.Flare flareItem = null;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i) instanceof unseen.items.Flare) {
                idx = i;
                flareItem = (unseen.items.Flare) inv.get(i);
                break;
            }
        }
        if (flareItem == null) {
            targetingFlare = false;
            repaint();
            return;
        }

        flareItem.useAt(player, map, gx, gy);
        inv.remove(idx);

        targetingFlare = false;

        GameState result = TurnManager.processTurn(player, enemies, map, smokes);
        setGameState(result);
        updateSmoke();
        requestFocusInWindow();
        repaint();
    }

    public void startGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        while (gameThread != null) {
            repaint();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public void setGameState(GameState state) {
        this.state = state;
    }

    public GameState getGameState() {
        return state;
    }

    public boolean attemptPickup() {
        int px = player.getX();
        int py = player.getY();
        Item it = map.getItem(px, py);
        if (it == null)
            return false;

        player.addItem(it);
        map.removeItem(px, py);
        System.out.println("Picked up: " + it.getClass().getSimpleName());

        // Using pickup consumes a turn — process enemy turns
        GameState result = TurnManager.processTurn(player, enemies, map, smokes);
        setGameState(result);
        updateSmoke();
        updateVisibility();
        return true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawMap(g);
        drawEntities(g);
        drawFlares(g);
        drawNoiseFlashes(g);
        if (targetingNoiseMaker || targetingFlare)
            drawTargetingOverlay(g);

        if (state == GameState.WIN) {
            // Dim overlay
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, getWidth(), getHeight());

            String line1 = "Floor " + floorNumber + " Complete!";
            g.setColor(Color.GREEN);
            g.setFont(new Font("Arial", Font.BOLD, 42));
            int w1 = g.getFontMetrics().stringWidth(line1);
            g.drawString(line1, (getWidth() - w1) / 2, getHeight() / 2 - 30);

            String line2 = "Press any key for Floor " + (floorNumber + 1);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 22));
            int w2 = g.getFontMetrics().stringWidth(line2);
            g.drawString(line2, (getWidth() - w2) / 2, getHeight() / 2 + 20);
        }

        if (state == GameState.LOSE) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, getWidth(), getHeight());

            String line1 = "GAME OVER";
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            int lw1 = g.getFontMetrics().stringWidth(line1);
            g.drawString(line1, (getWidth() - lw1) / 2, getHeight() / 2 - 30);

            String line2 = "Reached Floor " + floorNumber;
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 24));
            int lw2 = g.getFontMetrics().stringWidth(line2);
            g.drawString(line2, (getWidth() - lw2) / 2, getHeight() / 2 + 20);

            String line3 = "Press R to restart";
            g.setColor(new Color(200, 200, 200));
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            int lw3 = g.getFontMetrics().stringWidth(line3);
            g.drawString(line3, (getWidth() - lw3) / 2, getHeight() / 2 + 55);
        }

        if (state == GameState.PAUSED) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, getWidth(), getHeight());

            String pauseText = "PAUSED";
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            int pw = g.getFontMetrics().stringWidth(pauseText);
            g.drawString(pauseText, (getWidth() - pw) / 2, getHeight() / 2 - 20);

            String resumeHint = "Press ESC or P to resume";
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            int rw = g.getFontMetrics().stringWidth(resumeHint);
            g.setColor(new Color(200, 200, 200));
            g.drawString(resumeHint, (getWidth() - rw) / 2, getHeight() / 2 + 25);
        }

        // Floor number in top-right corner
        if (state == GameState.PLAYING) {
            String floorLabel = "Floor " + floorNumber;
            g.setFont(new Font("Arial", Font.BOLD, 18));
            int lw = g.getFontMetrics().stringWidth(floorLabel);
            int rx = getWidth() - lw - 14;
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRoundRect(rx - 6, 10, lw + 12, 26, 8, 8);
            g.setColor(new Color(255, 220, 100));
            g.drawString(floorLabel, rx, 28);
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
        int panelWidth = getWidth();
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
        boolean hasNoiseMaker = player.getInventory().stream().anyMatch(i -> i instanceof unseen.items.NoiseMaker);
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
        boolean hasSmokeBomb = player.getInventory().stream().anyMatch(i -> i instanceof unseen.items.SmokeBomb);
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
        boolean hasFlare = player.getInventory().stream().anyMatch(i -> i instanceof unseen.items.Flare);
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

        // 1) draw base tiles
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                Tile tile = map.getTile(x, y);
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
                Item ground = map.getItem(x, y);
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
                    } else if (ground instanceof unseen.items.Flare && AssetLoader.get().lantern != null) {
                        g2.drawImage(AssetLoader.get().lantern, tx + iconPad, ty + iconPad,
                                Constants.TILE_SIZE - 2 * iconPad, Constants.TILE_SIZE - 2 * iconPad, null);
                    }
                    // If image is missing, do not draw anything for the item.
                }
            }
        }

        // 3) draw smoke clouds as semi-transparent filled circles with a gentle pulse
        java.awt.Stroke savedStroke = g2.getStroke();
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

        // 4) overlay fog (darkening) for non-visible tiles
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                if (!visible[y][x]) {
                    g2.setColor(new Color(0, 0, 0, 200)); // semi-transparent black
                    g2.fillRect(
                            x * Constants.TILE_SIZE,
                            y * Constants.TILE_SIZE,
                            Constants.TILE_SIZE,
                            Constants.TILE_SIZE);
                }
            }
        }
    }

    private void drawEntities(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Player
        if (visible[player.getY()][player.getX()]) {
            if (player.getHeroImage() != null) {
                int scaledSize = Constants.TILE_SIZE * 2;
                int drawX = player.getX() * Constants.TILE_SIZE + (Constants.TILE_SIZE - scaledSize) / 2;
                int drawY = player.getY() * Constants.TILE_SIZE + (Constants.TILE_SIZE - scaledSize) / 2;
                if (player.getFacing() == unseen.entities.Player.Facing.LEFT) {
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
        for (Enemy e : enemies) {

            int ex = e.getX();
            int ey = e.getY();

            // Draw enemy only if visible
            if (visible[ey][ex]) {
                java.awt.Image img = e.getEnemyImage();
                if (img != null) {
                    int ts = Constants.TILE_SIZE;
                    int spriteSize = (e instanceof unseen.entities.PatrolEnemy)
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
                if (e instanceof unseen.entities.SentryEnemy) {
                    unseen.entities.SentryEnemy s = (unseen.entities.SentryEnemy) e;
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
                        && e.hasLineOfSightToPlayer(map, player, smokes)) {

                    java.util.List<unseen.ai.Node> path = e.getPlannedPath(map, player);

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
        if (flares.isEmpty())
            return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        long now = System.currentTimeMillis();
        int ts = Constants.TILE_SIZE;
        for (unseen.game.ActiveFlare f : flares) {
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
        if (noiseFlashes.isEmpty())
            return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        java.awt.Stroke saved = g2.getStroke();
        long now = System.currentTimeMillis();
        int ts = Constants.TILE_SIZE;
        for (FlashEffect f : noiseFlashes) {
            int cx = f.x * ts + ts / 2;
            int cy = f.y * ts + ts / 2;
            // Base alpha decreases as the flash ages (countdown 4 → 1)
            int baseAlpha = f.countdown * 55; // 220, 165, 110, 55
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
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Highlight hovered tile
        boolean validTile = mouseGridX >= 0 && mouseGridY >= 0
                && mouseGridX < Constants.GRID_WIDTH && mouseGridY < Constants.GRID_HEIGHT
                && map.isPassable(mouseGridX, mouseGridY);

        int tx = mouseGridX * ts;
        int ty = mouseGridY * ts;

        if (validTile) {
            if (targetingFlare) {
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
        String actionName = targetingFlare ? "flare" : "noise";
        String msg = validTile ? "Click to throw " + actionName + "  |  Esc to cancel"
                : "Invalid tile  |  Esc to cancel";
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        int msgW = fm.stringWidth(msg);
        int bx = (getWidth() - msgW) / 2 - 12;
        int by = getHeight() - 42;
        g2.setColor(new Color(20, 20, 20, 190));
        g2.fillRoundRect(bx, by, msgW + 24, 28, 10, 10);

        // Colors for valid tile
        Color validColor;
        if (targetingFlare) {
            validColor = new Color(255, 255, 120);
        } else {
            validColor = new Color(255, 220, 80);
        }

        g2.setColor(validTile ? validColor : new Color(220, 100, 100));
        g2.drawString(msg, bx + 12, by + 20);
    }

    public Player getPlayer() {
        return player;
    }

    public Map getMap() {
        return map;
    }

    public List<Smoke> getSmokes() {
        return smokes;
    }
}
