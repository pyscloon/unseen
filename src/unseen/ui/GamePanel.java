package unseen.ui;

import javax.swing.JPanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.Polygon;
import java.awt.BasicStroke;
import java.awt.Point;

import unseen.map.*;
import unseen.entities.*;
import unseen.utils.Constants;
import unseen.game.GameState;
import unseen.ai.AStar;
import unseen.ai.LineOfSight;
import unseen.game.TurnManager;
import unseen.items.*;
import unseen.game.Smoke;

public class GamePanel extends JPanel implements Runnable {

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
            private Image noiseMakerImage;
            private Image smokeBombImage;
        private javax.sound.sampled.Clip backgroundClip;
    // ...existing code...

    private Thread gameThread;
    private GameState state = GameState.PLAYING;

    private Map map;
    private Player player;
    private List<Enemy> enemies;
    private boolean[][] visible;
    private List<Smoke> smokes = new ArrayList<>();

    // Sprites
    private Image wallImage;
    private Image floorImage;
    private Image torchTileImage;

    {
        // Load tile/wall images
        try {
            java.net.URL wallUrl = Thread.currentThread().getContextClassLoader().getResource("unseen/assets/wall.png");
            if (wallUrl != null) wallImage = javax.imageio.ImageIO.read(wallUrl);
            java.net.URL floorUrl = Thread.currentThread().getContextClassLoader().getResource("unseen/assets/tile.png");
            if (floorUrl != null) floorImage = javax.imageio.ImageIO.read(floorUrl);
            java.net.URL torchUrl = Thread.currentThread().getContextClassLoader().getResource("unseen/assets/torch.png");
            if (torchUrl != null) torchTileImage = javax.imageio.ImageIO.read(torchUrl);

            // Load NoiseMaker asset if present, else null
            java.net.URL noiseUrl = Thread.currentThread().getContextClassLoader().getResource("unseen/assets/noise.png");
            if (noiseUrl != null) {
                noiseMakerImage = javax.imageio.ImageIO.read(noiseUrl);
                System.out.println("Loaded noise.png successfully.");
            } else {
                noiseMakerImage = null;
                System.out.println("noise.png not found in unseen/assets folder.");
            }

            // Load SmokeBomb asset if present, else null
            java.net.URL smokeUrl = Thread.currentThread().getContextClassLoader().getResource("unseen/assets/smoke.png");
            if (smokeUrl != null) {
                smokeBombImage = javax.imageio.ImageIO.read(smokeUrl);
                System.out.println("Loaded smoke.png successfully.");
            } else {
                smokeBombImage = null;
                System.out.println("smoke.png not found in unseen/assets folder.");
            }
        } catch (Exception e) {
            wallImage = null;
            floorImage = null;
            torchTileImage = null;
            noiseMakerImage = null;
            smokeBombImage = null;
            System.out.println("Error loading inventory images: " + e.getMessage());
        }
    }

    public GamePanel() {
        this.setPreferredSize(
                new Dimension(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(new InputHandler(this));
        this.setFocusable(true);

        setupGame();
        loadAndPlayBackgroundSound();
    }

    private void loadAndPlayBackgroundSound() {
        try {
            java.net.URL soundURL = getClass().getClassLoader().getResource("assets/background.wav");
            if (soundURL != null) {
                javax.sound.sampled.AudioInputStream audioIn = javax.sound.sampled.AudioSystem.getAudioInputStream(soundURL);
                backgroundClip = javax.sound.sampled.AudioSystem.getClip();
                backgroundClip.open(audioIn);
                backgroundClip.loop(javax.sound.sampled.Clip.LOOP_CONTINUOUSLY);
            }
        } catch (Exception e) {
            backgroundClip = null;
        }
    }

    private void setupGame() {

        map = MapGenerator.generate();
        ExitPlacer.placeExit(map);

        player = new Player(Constants.START_X, Constants.START_Y);
        visible = new boolean[Constants.GRID_HEIGHT][Constants.GRID_WIDTH];
        enemies = new ArrayList<>();

        AStar pathfinder = new AStar();

        enemies.add(new PatrolEnemy(10, 10, pathfinder));
        enemies.add(new HunterEnemy(15, 15, pathfinder));
        enemies.add(new SentryEnemy(5, 14, pathfinder));

        player.addItem(new NoiseMaker());
        player.addItem(new SmokeBomb());
        player.setPanel(this);
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

        //  Torch illumination (range 2)
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {

                if (map.getTile(x, y) == Tile.TORCH) {

                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dx = -2; dx <= 2; dx++) {

                            int nx = x + dx;
                            int ny = y + dy;

                            if (nx >= 0 && ny >= 0 &&
                                    nx < Constants.GRID_WIDTH &&
                                    ny < Constants.GRID_HEIGHT) {

                                visible[ny][nx] = true;
                            }
                        }
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
    }

    public void spawnSmoke(int x, int y) {
        smokes.add(new Smoke(x, y, 2, 5)); // radius 2, lasts 5 turns
    }

    public void startGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        while (gameThread != null) {
            repaint();
            try { Thread.sleep(100); }
            catch (InterruptedException ignored) {}
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
        if (it == null) return false;

        player.addItem(it);
        map.removeItem(px, py);
        System.out.println("Picked up: " + it.getClass().getSimpleName());

        // Using pickup consumes a turn — process enemy turns
        GameState result = TurnManager.processTurn(player, enemies, map, smokes);
        setGameState(result);
        updateSmoke();
        return true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        updateVisibility();   // important

        drawMap(g);
        drawEntities(g);

        if (state == GameState.WIN) {
            g.setColor(Color.GREEN);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("YOU WIN!", 250, 300);
        }

        if (state == GameState.LOSE) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("GAME OVER", 250, 300);
        }

        // Always draw inventory bar
        drawInventory(g);
    }
    // Draw the player's inventory at the top of the screen
    private void drawInventory(Graphics g) {
        int boxSize = 44;
        int spacing = 12;
        int slots = 2;
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
        int labelWidth = g2.getFontMetrics().stringWidth("Inventory");
        int labelX = startX + (barWidth - labelWidth) / 2 - 10;
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
            if (noiseMakerImage != null) {
                g2.drawImage(noiseMakerImage, x + iconPad, y + iconPad, boxSize - 2 * iconPad, boxSize - 2 * iconPad, null);
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
            if (smokeBombImage != null) {
                g2.drawImage(smokeBombImage, x + iconPad, y + iconPad, boxSize - 2 * iconPad, boxSize - 2 * iconPad, null);
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
                        if (wallImage != null) {
                            g2.drawImage(wallImage, drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                        } else {
                            g2.setColor(new Color(90, 90, 90));
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        }
                        break;
                    case FLOOR:
                    case START:
                        if (floorImage != null) {
                            g2.drawImage(floorImage, drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                        } else {
                            g2.setColor(new Color(170, 170, 170));
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        }
                        break;
                    case TORCH:
                        if (torchTileImage != null) {
                            g2.drawImage(torchTileImage, drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                        } else {
                            g2.setColor(new Color(255, 140, 0));
                            g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        }
                        break;
                    case EXIT:
                        g2.setColor(Color.YELLOW);
                        g2.fillRect(drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE);
                        break;
                    default:
                        if (floorImage != null) {
                            g2.drawImage(floorImage, drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE, null);
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
                    if (ground instanceof NoiseMaker && noiseMakerImage != null) {
                        g2.drawImage(noiseMakerImage, tx + iconPad, ty + iconPad, Constants.TILE_SIZE - 2 * iconPad, Constants.TILE_SIZE - 2 * iconPad, null);
                    } else if (ground instanceof SmokeBomb && smokeBombImage != null) {
                        g2.drawImage(smokeBombImage, tx + iconPad, ty + iconPad, Constants.TILE_SIZE - 2 * iconPad, Constants.TILE_SIZE - 2 * iconPad, null);
                    }
                    // If image is missing, do not draw anything for the item.
                }
            }
        }

        // 3) draw smoke overlays (visible effect)
        g2.setColor(new Color(120, 120, 120, 180)); // semi-transparent gray smoke
        for (Smoke smoke : smokes) {
            int r = smoke.getRadius();
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int nx = smoke.getX() + dx;
                    int ny = smoke.getY() + dy;
                    if (nx >= 0 && ny >= 0 && nx < Constants.GRID_WIDTH && ny < Constants.GRID_HEIGHT) {
                        g2.fillRect(
                                nx * Constants.TILE_SIZE,
                                ny * Constants.TILE_SIZE,
                                Constants.TILE_SIZE,
                                Constants.TILE_SIZE
                        );
                    }
                }
            }
        }

        // 4) overlay fog (darkening) for non-visible tiles
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                if (!visible[y][x]) {
                    g2.setColor(new Color(0, 0, 0, 200)); // semi-transparent black
                    g2.fillRect(
                            x * Constants.TILE_SIZE,
                            y * Constants.TILE_SIZE,
                            Constants.TILE_SIZE,
                            Constants.TILE_SIZE
                    );
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
                        Constants.TILE_SIZE
                );
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
                    int drawX = ex * Constants.TILE_SIZE;
                    int drawY = ey * Constants.TILE_SIZE;
                    g2.drawImage(img, drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                } else {
                    g2.setColor(Color.RED);
                    g2.fillOval(
                            ex * Constants.TILE_SIZE,
                            ey * Constants.TILE_SIZE,
                            Constants.TILE_SIZE,
                            Constants.TILE_SIZE
                    );
                }

                // Draw arrow only when:
                //  - enemy is currently CHASE
                //  - enemy actually has line-of-sight to the player right now
// Draw faint dotted path preview
                if (e.getState() == Enemy.State.CHASE
                        && e.hasLineOfSightToPlayer(map, player, smokes)) {

                    java.util.List<unseen.ai.Node> path =
                            e.getPlannedPath(map, player);

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

    public Player getPlayer() { return player; }
    public Map getMap() { return map; }
    public List<Smoke> getSmokes() { return smokes; }
}
