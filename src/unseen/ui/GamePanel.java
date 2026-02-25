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

public class GamePanel extends JPanel implements Runnable {

    private Thread gameThread;
    private GameState state = GameState.PLAYING;

    private Map map;
    private Player player;
    private List<Enemy> enemies;
    private boolean[][] visible;

    public GamePanel() {
        this.setPreferredSize(
                new Dimension(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(new InputHandler(this));
        this.setFocusable(true);

        setupGame();
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

        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {

                if (LineOfSight.hasLineOfSight(map, px, py, x, y, 6)) {
                    visible[y][x] = true;
                }
            }
        }
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
    }

    private void drawMap(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        Color wallColor  = new Color(90, 90, 90);
        Color floorColor = new Color(170, 170, 170);

        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {

                Tile tile = map.getTile(x, y);

                // Draw tile normally
                switch (tile) {
                    case WALL:
                        g2.setColor(wallColor);
                        break;
                    case FLOOR:
                    case START:
                        g2.setColor(floorColor);
                        break;
                    case EXIT:
                        g2.setColor(Color.YELLOW);
                        break;
                    default:
                        g2.setColor(floorColor);
                }

                g2.fillRect(
                        x * Constants.TILE_SIZE,
                        y * Constants.TILE_SIZE,
                        Constants.TILE_SIZE,
                        Constants.TILE_SIZE
                );

                // Overlay fog if not visible
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
            g.setColor(Color.CYAN);
            g.fillOval(
                    player.getX() * Constants.TILE_SIZE,
                    player.getY() * Constants.TILE_SIZE,
                    Constants.TILE_SIZE,
                    Constants.TILE_SIZE
            );
        }

        g.setColor(Color.RED);
        for (Entity e : enemies) {

            if (visible[e.getY()][e.getX()]) {
                g.fillOval(
                        e.getX() * Constants.TILE_SIZE,
                        e.getY() * Constants.TILE_SIZE,
                        Constants.TILE_SIZE,
                        Constants.TILE_SIZE
                );
            }
        }
    }

    public Player getPlayer() { return player; }
    public Map getMap() { return map; }
}
