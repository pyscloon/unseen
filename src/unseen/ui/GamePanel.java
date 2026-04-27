package unseen.ui;

import unseen.entities.Enemy;
import unseen.entities.Player;
import unseen.game.GameState;
import unseen.game.RunStats;
import unseen.game.Smoke;
import unseen.game.SmokeSpawner;
import unseen.game.TurnManager;
import unseen.items.Item;
import unseen.items.Shuriken;
import unseen.map.Map;
import unseen.ui.gamepanel.GameRenderer;
import unseen.ui.gamepanel.HudToast;
import unseen.ui.gamepanel.LevelManager;
import unseen.ui.gamepanel.ScreenShake;
import unseen.ui.gamepanel.TutorialManager;
import unseen.utils.Constants;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel implements Runnable, SmokeSpawner {

    private javax.sound.sampled.Clip backgroundClip;

    private Thread gameThread;
    private GameState state = GameState.MENU;

    private boolean targetingNoiseMaker = false;
    private boolean targetingFlare      = false;
    private int mouseGridX = 0;
    private int mouseGridY = 0;
    private int mouseX     = 0;
    private int mouseY     = 0;
    private boolean targetingShuriken = false;
    private int shurikenDx = 1, shurikenDy = 0;

    private final LevelManager    levelManager;
    private final GameRenderer    renderer;
    private final TutorialManager tutorial = new TutorialManager();
    private final ScreenShake      screenShake = new ScreenShake();
    private final RunStats         runStats = new RunStats();

    /** HUD toast queue — most recent at the end. */
    private final List<HudToast> toasts = new ArrayList<>();

    /** Millisecond timestamp of the last hit — used for red vignette flash. */
    private long lastHitTime = 0;

    public TutorialManager getTutorial() { return tutorial; }
    public int getMouseX()               { return mouseX; }
    public int getMouseY()               { return mouseY; }
    public RunStats getRunStats()         { return runStats; }
    public List<HudToast> getToasts()    { return toasts; }
    public long getLastHitTime()         { return lastHitTime; }

    public GamePanel() {
        this.setPreferredSize(new Dimension(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(new InputHandler(this));
        this.setFocusable(true);

        this.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseMoved(java.awt.event.MouseEvent e) {
                mouseX     = e.getX();
                mouseY     = e.getY();
                mouseGridX = e.getX() / Constants.TILE_SIZE;
                mouseGridY = e.getY() / Constants.TILE_SIZE;
                if (targetingNoiseMaker || targetingFlare) repaint();
            }
        });

        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                // Tutorial click handling
                if (tutorial.isActive()) {
                    boolean tutConsumed = tutorial.handleClick(e.getX(), e.getY(), getWidth(), getHeight());
                    if (tutConsumed) {
                        repaint();
                        return;
                    }
                    if (!tutorial.isActive() && state == GameState.MENU) {
                        startFromMenu();
                        return;
                    }
                }

                // Noise/Flare targeting clicks during gameplay
                if (targetingNoiseMaker)  confirmNoiseTarget(mouseGridX, mouseGridY);
                else if (targetingFlare)  confirmFlareTarget(mouseGridX, mouseGridY);
            }
        });

        levelManager = new LevelManager();
        renderer     = new GameRenderer(this, levelManager);
        levelManager.setupGame();
        loadAndPlayBackgroundSound();
    }

    private void loadAndPlayBackgroundSound() {
        try {
            java.net.URL soundURL = getClass().getClassLoader().getResource("assets/background.wav");
            if (soundURL != null) {
                javax.sound.sampled.AudioInputStream audioIn =
                        javax.sound.sampled.AudioSystem.getAudioInputStream(soundURL);
                backgroundClip = javax.sound.sampled.AudioSystem.getClip();
                backgroundClip.open(audioIn);
                backgroundClip.loop(javax.sound.sampled.Clip.LOOP_CONTINUOUSLY);
            }
        } catch (Exception e) { backgroundClip = null; }
    }

    // -------------------------------------------------------------------------
    // Toast notifications
    // -------------------------------------------------------------------------

    /** Shows a brief auto-fading toast at the bottom of the HUD. */
    public void showToast(String message) {
        toasts.add(new HudToast(message));
    }

    public void showToast(String message, Color color) {
        toasts.add(new HudToast(message, color));
    }

    // -------------------------------------------------------------------------
    // Game lifecycle
    // -------------------------------------------------------------------------

    public void pauseGame()  { if (state == GameState.PLAYING) { state = GameState.PAUSED;  repaint(); } }
    public void resumeGame() {
        if (state == GameState.PAUSED || state == GameState.CONFIRM_QUIT) {
            state = GameState.PLAYING;
            requestFocusInWindow();
            repaint();
        }
    }

    /** Transitions from PAUSED -> CONFIRM_QUIT, showing the "Return to menu?" prompt. */
    public void showQuitConfirm() {
        if (state == GameState.PAUSED) {
            state = GameState.CONFIRM_QUIT;
            repaint();
        }
    }

    /** Tears down the current run and returns to the main menu. */
    public void returnToMenu() {
        runStats.commitHighScore();
        levelManager.setupGame();
        state = GameState.MENU;
        if (backgroundClip != null) {
            backgroundClip.setFramePosition(0);
            backgroundClip.loop(javax.sound.sampled.Clip.LOOP_CONTINUOUSLY);
        }
        requestFocusInWindow();
        repaint();
    }

    public void restartGame() {
        runStats.commitHighScore();
        runStats.resetRun();
        levelManager.setupGame();
        levelManager.getPlayer().resetHealth();
        setGameState(GameState.PLAYING);
        requestFocusInWindow();
        if (backgroundClip != null) {
            backgroundClip.setFramePosition(0);
            backgroundClip.loop(javax.sound.sampled.Clip.LOOP_CONTINUOUSLY);
        }
        repaint();
    }

    public void startFromMenu() {
        runStats.resetRun();
        levelManager.setupGame();
        setGameState(GameState.PLAYING);
        requestFocusInWindow();
        repaint();
    }

    public void nextFloor() {
        runStats.setFloorsCleared(levelManager.getFloorNumber());
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
        // ~30 FPS - smooth enough for screen-shake and HUD animations
        final long TARGET_MS = 33L;
        while (gameThread != null) {
            long start = System.currentTimeMillis();
            repaint();
            long elapsed = System.currentTimeMillis() - start;
            long sleep = TARGET_MS - elapsed;
            if (sleep > 0) {
                try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            }
        }
    }

    // -------------------------------------------------------------------------
    // Turn processing (shared helper)
    // -------------------------------------------------------------------------

    /**
     * Runs a turn via TurnManager.processTurnEx and handles all common
     * side-effects: stats, damage feedback, toasts, visibility.
     */
    public void processTurnAndApply() {
        TurnManager.TurnResult result = TurnManager.processTurnEx(
                levelManager.getPlayer(), levelManager.getEnemies(),
                levelManager.getMap(), levelManager.getSmokes());

        runStats.incrementTurns();
        runStats.incrementKills(result.killsThisTurn);
        runStats.setFloorsCleared(levelManager.getFloorNumber() - 1);

        if (result.playerHit) {
            lastHitTime = System.currentTimeMillis();
            int hp = levelManager.getPlayer().getHealth();
            if (hp > 0) {
                screenShake.trigger(12, 6f);
                showToast("HIT! " + hp + " HP remaining", new Color(255, 80, 60));
            }
        }

        setGameState(result.state);
        levelManager.updateSmoke();
    }

    // -------------------------------------------------------------------------
    // Targeting
    // -------------------------------------------------------------------------

    public void enterNoiseMakerTargeting() { targetingNoiseMaker = true; repaint(); }
    public void enterFlareTargeting()      { targetingFlare = true;      repaint(); }

    public void cancelTargeting() {
        targetingNoiseMaker = false;
        targetingFlare      = false;
        targetingShuriken   = false;
        repaint();
    }

    public boolean isTargetingNoiseMaker() { return targetingNoiseMaker; }
    public boolean isTargetingFlare()      { return targetingFlare; }

    private void confirmNoiseTarget(int gx, int gy) {
        if (gx < 0 || gy < 0 || gx >= Constants.GRID_WIDTH || gy >= Constants.GRID_HEIGHT) return;
        if (!levelManager.getMap().isPassable(gx, gy)) return;

        java.util.List<unseen.items.Item> inv = levelManager.getPlayer().getInventory();
        int idx = -1; unseen.items.NoiseMaker nm = null;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i) instanceof unseen.items.NoiseMaker) {
                idx = i; nm = (unseen.items.NoiseMaker) inv.get(i); break;
            }
        }
        if (nm == null) { targetingNoiseMaker = false; repaint(); return; }

        nm.useAt(levelManager.getPlayer(), levelManager.getMap(), levelManager.getEnemies(), gx, gy);
        inv.remove(idx);
        levelManager.addNoiseFlash(gx, gy);
        targetingNoiseMaker = false;

        processTurnAndApply();
        requestFocusInWindow();
        repaint();
    }

    private void confirmFlareTarget(int gx, int gy) {
        if (gx < 0 || gy < 0 || gx >= Constants.GRID_WIDTH || gy >= Constants.GRID_HEIGHT) return;
        if (!levelManager.getMap().isPassable(gx, gy)) return;

        java.util.List<unseen.items.Item> inv = levelManager.getPlayer().getInventory();
        int idx = -1; unseen.items.Flare flareItem = null;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i) instanceof unseen.items.Flare) {
                idx = i; flareItem = (unseen.items.Flare) inv.get(i); break;
            }
        }
        if (flareItem == null) { targetingFlare = false; repaint(); return; }

        flareItem.useAt(levelManager.getPlayer(), levelManager.getMap(), gx, gy);
        inv.remove(idx);
        targetingFlare = false;

        processTurnAndApply();
        requestFocusInWindow();
        repaint();
    }

    public void confirmShurikenThrow() {
        List<Item> inv = levelManager.getPlayer().getInventory();
        int idx = -1; Shuriken shuriken = null;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i) instanceof Shuriken) { idx = i; shuriken = (Shuriken) inv.get(i); break; }
        }
        if (idx < 0) { targetingShuriken = false; return; }

        int px = levelManager.getPlayer().getX();
        int py = levelManager.getPlayer().getY();
        shuriken.fireInDirection(px, py, shurikenDx, shurikenDy,
                levelManager.getMap(), levelManager.getEnemies());
        inv.remove(idx);
        targetingShuriken = false;

        // Determine how far the shuriken actually flew and spawn the visual
        int[] killPos = shuriken.getLastKillPos();
        int travelTiles;
        if (killPos != null) {
            travelTiles = Math.abs(killPos[0] - px) + Math.abs(killPos[1] - py);
        } else {
            // Stopped at wall or edge -- count the unobstructed tiles
            travelTiles = 0;
            for (int i = 1; i <= unseen.items.Shuriken.RANGE; i++) {
                int tx = px + shurikenDx * i;
                int ty = py + shurikenDy * i;
                if (tx < 0 || tx >= unseen.utils.Constants.GRID_WIDTH
                        || ty < 0 || ty >= unseen.utils.Constants.GRID_HEIGHT) break;
                if (levelManager.getMap().getTile(tx, ty) == unseen.map.Tile.WALL) break;
                travelTiles = i;
            }
        }
        if (travelTiles > 0) {
            levelManager.spawnShurikenFlight(px, py, shurikenDx, shurikenDy, travelTiles);
        }

        if (killPos != null) {
            spawnDeathPuff(killPos[0], killPos[1]);
        }

        processTurnAndApply();
        requestFocusInWindow();
        repaint();
    }

    // -------------------------------------------------------------------------
    // Smoke / flare / death puff
    // -------------------------------------------------------------------------

    @Override public void spawnSmoke(int x, int y) { levelManager.spawnSmoke(x, y); }
    @Override public void spawnFlare(int x, int y) { levelManager.spawnFlare(x, y); }

    public void spawnDeathPuff(int x, int y) { levelManager.spawnDeathPuff(x, y); }
    public void updateSmoke()                { levelManager.updateSmoke(); }

    // -------------------------------------------------------------------------
    // Pickup
    // -------------------------------------------------------------------------

    public boolean attemptPickup() {
        int px = levelManager.getPlayer().getX();
        int py = levelManager.getPlayer().getY();
        Item it = levelManager.getMap().getItem(px, py);
        if (it == null) return false;

        levelManager.getPlayer().addItem(it);
        levelManager.getMap().removeItem(px, py);
        showToast("Picked up " + it.getClass().getSimpleName(), new Color(120, 255, 160));

        processTurnAndApply();
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

    public void setGameState(GameState s) {
        this.state = s;
        if (s == GameState.LOSE) {
            screenShake.trigger(18, 8f);
            runStats.setFloorsCleared(levelManager.getFloorNumber() - 1);
            runStats.commitHighScore();
        }
    }
    public GameState getGameState()       { return state; }

    /** Manually trigger a screen shake (frames, magnitude in pixels). */
    public void triggerShake(int frames, float magnitude) { screenShake.trigger(frames, magnitude); }
    public ScreenShake getScreenShake()                   { return screenShake; }

    public Player getPlayer()       { return levelManager.getPlayer(); }
    public Map getMap()             { return levelManager.getMap(); }
    public List<Enemy> getEnemies() { return levelManager.getEnemies(); }
    public List<Smoke> getSmokes()  { return levelManager.getSmokes(); }
    public java.util.List<unseen.game.StickyTrap> getTraps() { return levelManager.getTraps(); }
    public java.util.List<unseen.ui.gamepanel.ShurikenProjectile> getShurikenProjectiles() {
        return levelManager.getShurikenProjectiles();
    }

    public int getMouseGridX() { return mouseGridX; }
    public int getMouseGridY() { return mouseGridY; }

    public boolean isTargetingShuriken() { return targetingShuriken; }
    public int getShurikenDx()           { return shurikenDx; }
    public int getShurikenDy()           { return shurikenDy; }

    public void enterShurikenTargeting() {
        shurikenDx = (levelManager.getPlayer().getFacing() == Player.Facing.RIGHT) ? 1 : -1;
        shurikenDy = 0;
        targetingShuriken = true;
        repaint();
    }

    public void setShurikenDirection(int dx, int dy) {
        shurikenDx = dx;
        shurikenDy = dy;
        repaint();
    }
}