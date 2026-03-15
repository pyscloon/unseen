package unseen.ui;

import unseen.entities.Enemy;
import unseen.entities.Player;
import unseen.game.GameState;
import unseen.game.Smoke;
import unseen.game.SmokeSpawner;
import unseen.game.TurnManager;
import unseen.items.Item;
import unseen.map.Map;
import unseen.ui.gamepanel.GameRenderer;
import unseen.ui.gamepanel.LevelManager;
import unseen.utils.Constants;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GamePanel extends JPanel implements Runnable, SmokeSpawner {

    private javax.sound.sampled.Clip backgroundClip;

    private Thread gameThread;
    private GameState state = GameState.MENU;

    // Noise-maker targeting mode
    private boolean targetingNoiseMaker = false;
    // Flare targeting mode
    private boolean targetingFlare = false;
    private int mouseGridX = 0;
    private int mouseGridY = 0;

    private final LevelManager levelManager;
    private final GameRenderer renderer;

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

        levelManager = new LevelManager();
        renderer = new GameRenderer(this, levelManager);
        // Build a preview floor so the main menu can render an in-game backdrop.
        levelManager.setupGame();
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

    // -------------------------------------------------------------------------
    // Game lifecycle
    // -------------------------------------------------------------------------

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
        levelManager.setupGame();
        setGameState(GameState.PLAYING);
        requestFocusInWindow();
        if (backgroundClip != null) {
            backgroundClip.setFramePosition(0);
            backgroundClip.loop(javax.sound.sampled.Clip.LOOP_CONTINUOUSLY);
        }
        repaint();
    }

    /** Starts a new run from the main menu. */
    public void startFromMenu() {
        levelManager.setupGame();
        setGameState(GameState.PLAYING);
        requestFocusInWindow();
        repaint();
    }

    /**
     * Advance to the next floor.
     */
    public void nextFloor() {
        levelManager.nextFloor();
        setGameState(GameState.PLAYING);
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

    // -------------------------------------------------------------------------
    // Targeting
    // -------------------------------------------------------------------------

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

    /** Called when the player clicks a tile while in noise-maker targeting mode. */
    private void confirmNoiseTarget(int gx, int gy) {
        // Must be within bounds and on a passable tile
        if (gx < 0 || gy < 0 || gx >= Constants.GRID_WIDTH || gy >= Constants.GRID_HEIGHT)
            return;
        if (!levelManager.getMap().isPassable(gx, gy))
            return;

        // Find the NoiseMaker in inventory
        java.util.List<unseen.items.Item> inv = levelManager.getPlayer().getInventory();
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
        nm.useAt(levelManager.getPlayer(), levelManager.getMap(), levelManager.getEnemies(), gx, gy);
        inv.remove(idx);
        levelManager.addNoiseFlash(gx, gy);

        targetingNoiseMaker = false;

        GameState result = TurnManager.processTurn(levelManager.getPlayer(), levelManager.getEnemies(),
                levelManager.getMap(), levelManager.getSmokes());
        setGameState(result);
        levelManager.updateSmoke();
        requestFocusInWindow();
        repaint();
    }

    private void confirmFlareTarget(int gx, int gy) {
        // Must be within bounds and on a passable tile
        if (gx < 0 || gy < 0 || gx >= Constants.GRID_WIDTH || gy >= Constants.GRID_HEIGHT)
            return;
        if (!levelManager.getMap().isPassable(gx, gy))
            return;

        // Find the Flare in inventory
        java.util.List<unseen.items.Item> inv = levelManager.getPlayer().getInventory();
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

        flareItem.useAt(levelManager.getPlayer(), levelManager.getMap(), gx, gy);
        inv.remove(idx);

        targetingFlare = false;

        GameState result = TurnManager.processTurn(levelManager.getPlayer(), levelManager.getEnemies(),
                levelManager.getMap(), levelManager.getSmokes());
        setGameState(result);
        levelManager.updateSmoke();
        requestFocusInWindow();
        repaint();
    }

    // -------------------------------------------------------------------------
    // Smoke / flare spawning (SmokeSpawner implementation delegates to
    // LevelManager)
    // -------------------------------------------------------------------------

    @Override
    public void spawnSmoke(int x, int y) {
        levelManager.spawnSmoke(x, y);
    }

    @Override
    public void spawnFlare(int x, int y) {
        levelManager.spawnFlare(x, y);
    }

    public void updateSmoke() {
        levelManager.updateSmoke();
    }

    // -------------------------------------------------------------------------
    // Pickup
    // -------------------------------------------------------------------------

    public boolean attemptPickup() {
        int px = levelManager.getPlayer().getX();
        int py = levelManager.getPlayer().getY();
        Item it = levelManager.getMap().getItem(px, py);
        if (it == null)
            return false;

        levelManager.getPlayer().addItem(it);
        levelManager.getMap().removeItem(px, py);
        System.out.println("Picked up: " + it.getClass().getSimpleName());

        // Using pickup consumes a turn — process enemy turns
        GameState result = TurnManager.processTurn(levelManager.getPlayer(), levelManager.getEnemies(),
                levelManager.getMap(), levelManager.getSmokes());
        setGameState(result);
        levelManager.updateSmoke();
        levelManager.updateVisibility();
        return true;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.render(g);
    }

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

    public void setGameState(GameState state) {
        this.state = state;
    }

    public GameState getGameState() {
        return state;
    }

    public Player getPlayer() {
        return levelManager.getPlayer();
    }

    public Map getMap() {
        return levelManager.getMap();
    }

    public List<Enemy> getEnemies() {
        return levelManager.getEnemies();
    }

    public List<Smoke> getSmokes() {
        return levelManager.getSmokes();
    }

    public java.util.List<unseen.game.StickyTrap> getTraps() {
        return levelManager.getTraps();
    }

    public int getMouseGridX() {
        return mouseGridX;
    }

    public int getMouseGridY() {
        return mouseGridY;
    }
}
