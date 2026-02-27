package unseen.ui;

import javax.swing.JPanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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
        } catch (Exception e) {
            wallImage = null;
            floorImage = null;
            torchTileImage = null;
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

        // Draw inventory at the top
        drawInventory(g);
    }
    // Draw the player's inventory at the top of the screen
    private void drawInventory(Graphics g) {
        List<Item> inventory = player.getInventory();
        int boxSize = 44;
        int spacing = 12;
        int minSlots = 5;
        int slots = Math.max(minSlots, inventory.size());
        int barWidth = Math.max(220, slots * (boxSize + spacing) + 40);
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
        // Draw item slots
        int x = startX;
        for (int i = 0; i < slots; i++) {
            // Highlight selected slot (optional: slot 1)
            if (i == 0) {
                g2.setColor(new Color(255, 255, 180, 180));
                g2.setStroke(new java.awt.BasicStroke(3f));
                g2.drawRoundRect(x - 2, y - 2, boxSize + 4, boxSize + 4, 12, 12);
            }
            // Draw slot background
            g2.setColor(new Color(70, 70, 70, 220));
            g2.fillRoundRect(x, y, boxSize, boxSize, 12, 12);
            g2.setColor(new Color(180, 180, 180));
            g2.setStroke(new java.awt.BasicStroke(2f));
            g2.drawRoundRect(x, y, boxSize, boxSize, 12, 12);
            // Draw item if present
            if (i < inventory.size()) {
                Item item = inventory.get(i);
                String name = item.getClass().getSimpleName();
                // Shadowed text for item name
                g2.setFont(new Font("Arial", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(name);
                int textX = x + (boxSize - textWidth) / 2;
                int textY = y + boxSize / 2 + fm.getAscent() / 2 - 4;
                g2.setColor(new Color(0,0,0,180));
                g2.drawString(name, textX + 1, textY + 1);
                g2.setColor(new Color(255,255,255));
                g2.drawString(name, textX, textY);
            }
            // Draw slot number
            g2.setFont(new Font("Arial", Font.PLAIN, 11));
            g2.setColor(new Color(200, 200, 200, 180));
            g2.drawString(String.valueOf(i + 1), x + boxSize - 13, y + boxSize - 6);
            x += boxSize + spacing;
        }
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

        // 2) draw items on ground (dimmed by visibility)
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                Item ground = map.getItem(x, y);
                if (ground != null && visible[y][x]) {
                    int tx = x * Constants.TILE_SIZE;
                    int ty = y * Constants.TILE_SIZE;
                    int size = Constants.TILE_SIZE / 2;
                    int ox = (Constants.TILE_SIZE - size) / 2;
                    int oy = (Constants.TILE_SIZE - size) / 2;

                    if (ground instanceof NoiseMaker) g2.setColor(new Color(200, 180, 50)); // gold-ish
                    else if (ground instanceof SmokeBomb) g2.setColor(new Color(180, 180, 180)); // grey smoke icon
                    else g2.setColor(Color.MAGENTA);

                    g2.fillOval(tx + ox, ty + oy, size, size);
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

        // Only draw if visible
        if (visible[player.getY()][player.getX()]) {
            if (player.getHeroImage() != null) {
                int scaledSize = Constants.TILE_SIZE * 2;
                int drawX = player.getX() * Constants.TILE_SIZE + (Constants.TILE_SIZE - scaledSize) / 2;
                int drawY = player.getY() * Constants.TILE_SIZE + (Constants.TILE_SIZE - scaledSize) / 2;
                g.drawImage(player.getHeroImage(),
                    drawX,
                    drawY,
                    scaledSize,
                    scaledSize,
                    null);
            } else {
                g.setColor(Color.CYAN);
                g.fillOval(
                    player.getX() * Constants.TILE_SIZE,
                    player.getY() * Constants.TILE_SIZE,
                    Constants.TILE_SIZE,
                    Constants.TILE_SIZE
                );
            }
        }
        for (Enemy e : enemies) {
            if (visible[e.getY()][e.getX()]) {
                java.awt.Image img = e.getEnemyImage();
                if (img != null) {
                    int drawX = e.getX() * Constants.TILE_SIZE;
                    int drawY = e.getY() * Constants.TILE_SIZE;
                    g.drawImage(img, drawX, drawY, Constants.TILE_SIZE, Constants.TILE_SIZE, null);
                } else {
                    g.setColor(Color.RED);
                    g.fillOval(
                        e.getX() * Constants.TILE_SIZE,
                        e.getY() * Constants.TILE_SIZE,
                        Constants.TILE_SIZE,
                        Constants.TILE_SIZE
                    );
                }
            }
        }
    }

    public Player getPlayer() { return player; }
    public Map getMap() { return map; }
    public List<Smoke> getSmokes() { return smokes; }
}
