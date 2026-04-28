package unseen.ui;

import unseen.entities.Enemy;
import unseen.entities.Player;
import unseen.game.GameState;
import unseen.game.RunStats;
import unseen.game.Smoke;
import unseen.game.SmokeSpawner;
import unseen.game.TurnManager;
import unseen.items.Item;
import unseen.items.GrapplingHook;
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
    private boolean targetingFlare = false;
    private boolean targetingGrapplingHook = false;
    private int targetGridX = 0;
    private int targetGridY = 0;
    private int mouseX = 0;
    private int mouseY = 0;
    private boolean targetingShuriken = false;
    private int shurikenDx = 1, shurikenDy = 0;

    // -- Grapple Animation --
    private boolean grappling = false;
    private float grappleProgress = 0f;
    private int grappleStartX, grappleStartY;
    private int grappleEndX, grappleEndY;
    private int grappleWallX, grappleWallY;
    private unseen.items.Item grappleUsedItem = null;

    private final LevelManager levelManager;
    private final GameRenderer renderer;
    private final TutorialManager tutorial = new TutorialManager();
    private final ScreenShake screenShake = new ScreenShake();
    private final RunStats runStats = new RunStats();

    /** HUD toast queue -- most recent at the end. */
    private final List<HudToast> toasts = new ArrayList<>();

    /** Millisecond timestamp of the last hit -- used for red vignette flash. */
    private long lastHitTime = 0;

    private boolean horrorMode = false;
    private String currentNoteLore = null;

    public boolean isHorrorMode() { return horrorMode; }
    public String getCurrentNoteLore() { return currentNoteLore; }
    public void setCurrentNoteLore(String lore) { this.currentNoteLore = lore; }
    public void setHorrorMode(boolean hm) { 
        boolean changed = (this.horrorMode != hm);
        this.horrorMode = hm; 
        if (changed) {
            currentTrackIndex = 0;
            loadAndPlayBackgroundSound();
        }
    }

    public TutorialManager getTutorial() {
        return tutorial;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public RunStats getRunStats() {
        return runStats;
    }

    public List<HudToast> getToasts() {
        return toasts;
    }

    public long getLastHitTime() {
        return lastHitTime;
    }

    public GamePanel() {
        this.setPreferredSize(new Dimension(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(new InputHandler(this));
        this.setFocusable(true);

        this.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                targetGridX = e.getX() / Constants.TILE_SIZE;
                targetGridY = e.getY() / Constants.TILE_SIZE;
                if (targetingNoiseMaker || targetingFlare || targetingGrapplingHook)
                    repaint();
            }
        });

        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
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
                if (targetingNoiseMaker)
                    confirmNoiseTarget(targetGridX, targetGridY);
                else if (targetingFlare)
                    confirmFlareTarget(targetGridX, targetGridY);
                else if (targetingGrapplingHook)
                    confirmGrapplingHookTarget(targetGridX, targetGridY);
            }
        });

        levelManager = new LevelManager();
        levelManager.setPanel(this);
        renderer = new GameRenderer(this, levelManager);
        levelManager.setupGame();
        loadAndPlayBackgroundSound();
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    private final String[] playlist = { "unseen/assets/sound/caves1.wav", "unseen/assets/sound/caves2.wav" };
    private final String[] horrorPlaylist = { "unseen/assets/sound/horror-mode/horror-bgmusic.wav" };
    private int currentTrackIndex = 0;
    private boolean musicEnabled = true;

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void toggleMusic() {
        musicEnabled = !musicEnabled;
        if (!musicEnabled) {
            if (backgroundClip != null)
                backgroundClip.stop();
        } else {
            if (backgroundClip != null) {
                backgroundClip.start();
            } else {
                loadAndPlayBackgroundSound();
            }
        }
    }

    private void loadAndPlayBackgroundSound() {
        if (backgroundClip != null) {
            backgroundClip.stop();
            backgroundClip.close();
        }

        try {
            boolean isActuallyHorror = horrorMode && !levelManager.isFloorPurified();
            String[] activePlaylist = isActuallyHorror ? horrorPlaylist : playlist;
            if (currentTrackIndex >= activePlaylist.length) currentTrackIndex = 0;
            
            String path = activePlaylist[currentTrackIndex];
            java.net.URL soundURL = getClass().getClassLoader().getResource(path);

            if (soundURL == null) {
                // Try the other one if this one is missing (e.g. still downloading)
                currentTrackIndex = (currentTrackIndex + 1) % activePlaylist.length;
                path = activePlaylist[currentTrackIndex];
                soundURL = getClass().getClassLoader().getResource(path);
            }

            if (soundURL != null) {
                javax.sound.sampled.AudioInputStream audioIn = javax.sound.sampled.AudioSystem
                        .getAudioInputStream(soundURL);
                backgroundClip = javax.sound.sampled.AudioSystem.getClip();
                backgroundClip.open(audioIn);

                // Add listener to alternate when finished
                backgroundClip.addLineListener(event -> {
                    if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                        // Check if it reached the end (not stopped manually)
                        if (backgroundClip != null
                                && backgroundClip.getFramePosition() >= backgroundClip.getFrameLength()) {
                            currentTrackIndex = (currentTrackIndex + 1) % playlist.length;
                            // Need to run this on a separate thread or invoke later because
                            // we're currently in a callback that might hold locks.
                            SwingUtilities.invokeLater(this::loadAndPlayBackgroundSound);
                        }
                    }
                });

                // Apply volume (reduced for horror mode to keep it atmospheric)
                if (backgroundClip.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
                    javax.sound.sampled.FloatControl gainControl = (javax.sound.sampled.FloatControl) backgroundClip
                            .getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                    float volume = horrorMode ? 0.2f : 0.4f;
                    float dB = (float) (Math.log(Math.max(volume, 0.0001)) / Math.log(10.0) * 20.0);
                    gainControl.setValue(Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB)));
                }

                if (musicEnabled) {
                    backgroundClip.start();
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "Audio Error: Could not play " + playlist[currentTrackIndex] + ". Details: " + e.getMessage());
            backgroundClip = null;
        }
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

    public void pauseGame() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
            repaint();
        }
    }

    public void resumeGame() {
        if (state == GameState.PAUSED || state == GameState.CONFIRM_QUIT) {
            state = GameState.PLAYING;
            requestFocusInWindow();
            repaint();
        }
    }

    /**
     * Transitions from PAUSED -> CONFIRM_QUIT, showing the "Return to menu?"
     * prompt.
     */
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
            backgroundClip.start();
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
            backgroundClip.start();
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
        unseen.utils.SoundManager.get().play("ladder");
        runStats.setFloorsCleared(levelManager.getFloorNumber());
        levelManager.nextFloor();
        loadAndPlayBackgroundSound(); // Restore horror music if applicable
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
            
            if (state == GameState.PLAYING && horrorMode) {
                levelManager.updateVisibility();
            }

            if (grappling) {
                updateGrappling();
            }
            
            repaint();
            long elapsed = System.currentTimeMillis() - start;
            long sleep = TARGET_MS - elapsed;
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ignored) {
                }
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
                this,
                levelManager.getPlayer(),
                levelManager.getEnemies(),
                levelManager.getMap(),
                levelManager.getSmokes());

        runStats.incrementTurns();
        levelManager.incrementTurn();
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

    public void enterNoiseMakerTargeting() {
        targetingNoiseMaker = true;
        targetGridX = levelManager.getPlayer().getX();
        targetGridY = levelManager.getPlayer().getY();
        repaint();
    }

    public void enterFlareTargeting() {
        targetingFlare = true;
        targetGridX = levelManager.getPlayer().getX();
        targetGridY = levelManager.getPlayer().getY();
        repaint();
    }

    public void enterGrapplingHookTargeting() {
        targetingGrapplingHook = true;
        targetGridX = levelManager.getPlayer().getX();
        targetGridY = levelManager.getPlayer().getY();
        repaint();
    }

    public void moveTarget(int dx, int dy) {
        targetGridX += dx;
        targetGridY += dy;
        // Clamp to map bounds
        targetGridX = Math.max(0, Math.min(Constants.GRID_WIDTH - 1, targetGridX));
        targetGridY = Math.max(0, Math.min(Constants.GRID_HEIGHT - 1, targetGridY));
        repaint();
    }

    public void confirmTargeting() {
        if (targetingNoiseMaker) {
            confirmNoiseTarget(targetGridX, targetGridY);
        } else if (targetingFlare) {
            confirmFlareTarget(targetGridX, targetGridY);
        } else if (targetingGrapplingHook) {
            confirmGrapplingHookTarget(targetGridX, targetGridY);
        }
    }

    public void cancelTargeting() {
        targetingNoiseMaker = false;
        targetingFlare = false;
        targetingShuriken = false;
        targetingGrapplingHook = false;
        repaint();
    }

    public boolean isTargetingNoiseMaker() {
        return targetingNoiseMaker;
    }

    public boolean isTargetingFlare() {
        return targetingFlare;
    }

    public boolean isTargetingGrapplingHook() {
        return targetingGrapplingHook;
    }

    private void confirmNoiseTarget(int gx, int gy) {
        if (gx < 0 || gy < 0 || gx >= Constants.GRID_WIDTH || gy >= Constants.GRID_HEIGHT)
            return;
        if (!levelManager.getMap().isPassable(gx, gy))
            return;

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
        unseen.utils.SoundManager.get().play("noisemaker");
        nm.useAt(levelManager.getPlayer(), levelManager.getMap(), levelManager.getEnemies(), gx, gy);
        inv.remove(idx);
        levelManager.addNoiseFlash(gx, gy);
        targetingNoiseMaker = false;

        processTurnAndApply();
        requestFocusInWindow();
        repaint();
    }

    private void confirmFlareTarget(int gx, int gy) {
        if (gx < 0 || gy < 0 || gx >= Constants.GRID_WIDTH || gy >= Constants.GRID_HEIGHT)
            return;
        if (!levelManager.getMap().isPassable(gx, gy))
            return;

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

        if (!horrorMode) {
            unseen.utils.SoundManager.get().play("lantern");
        }
        flareItem.useAt(levelManager.getPlayer(), levelManager.getMap(), gx, gy);
        inv.remove(idx);
        targetingFlare = false;

        processTurnAndApply();
        requestFocusInWindow();
        repaint();
    }

    private void confirmGrapplingHookTarget(int gx, int gy) {
        if (gx < 0 || gy < 0 || gx >= Constants.GRID_WIDTH || gy >= Constants.GRID_HEIGHT)
            return;

        java.util.List<unseen.items.Item> inv = levelManager.getPlayer().getInventory();
        int idx = -1;
        GrapplingHook hook = null;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i) instanceof GrapplingHook) {
                idx = i;
                hook = (GrapplingHook) inv.get(i);
                break;
            }
        }
        if (hook == null) {
            targetingGrapplingHook = false;
            repaint();
            return;
        }

        if (!hook.isWallTarget(levelManager.getMap(), gx, gy)) {
            showToast("Must target a wall!", new Color(200, 150, 50));
            return;
        }

        // Check if useAt would succeed (landing spot check)
        int[] landing = hook.findLandingSpot(levelManager.getPlayer(), levelManager.getMap(), levelManager.getEnemies(), gx, gy);
        if (landing != null) {
            // Start animation instead of immediate move
            unseen.utils.SoundManager.get().play("grapple", 0.8f);
            
            grappling = true;
            grappleProgress = 0f;
            grappleStartX = levelManager.getPlayer().getX();
            grappleStartY = levelManager.getPlayer().getY();
            grappleEndX = landing[0];
            grappleEndY = landing[1];
            grappleWallX = gx;
            grappleWallY = gy;
            grappleUsedItem = inv.remove(idx);
            
            targetingGrapplingHook = false;
        } else {
            showToast("No landing spot available!", new Color(200, 100, 50));
        }
        repaint();
    }

    private void updateGrappling() {
        grappleProgress += 0.15f; // Animation speed
        if (grappleProgress >= 1.0f) {
            grappleProgress = 1.0f;
            grappling = false;
            
            // Apply the actual movement now
            levelManager.getPlayer().setPosition(grappleEndX, grappleEndY);
            // Update facing
            if (grappleEndX < grappleStartX) levelManager.getPlayer().setFacing(Player.Facing.LEFT);
            else if (grappleEndX > grappleStartX) levelManager.getPlayer().setFacing(Player.Facing.RIGHT);
            
            showToast("Grapple zip!", new Color(150, 220, 255));
            processTurnAndApply();
            requestFocusInWindow();
        }
        repaint();
    }

    public boolean isGrappling() { return grappling; }
    public float getGrappleProgress() { return grappleProgress; }
    public int getGrappleStartX() { return grappleStartX; }
    public int getGrappleStartY() { return grappleStartY; }
    public int getGrappleWallX() { return grappleWallX; }
    public int getGrappleWallY() { return grappleWallY; }
    public int getGrappleEndX() { return grappleEndX; }
    public int getGrappleEndY() { return grappleEndY; }

    public void confirmShurikenThrow() {
        List<Item> inv = levelManager.getPlayer().getInventory();
        int idx = -1;
        Shuriken shuriken = null;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i) instanceof Shuriken) {
                idx = i;
                shuriken = (Shuriken) inv.get(i);
                break;
            }
        }
        if (idx < 0) {
            targetingShuriken = false;
            return;
        }

        int px = levelManager.getPlayer().getX();
        int py = levelManager.getPlayer().getY();
        unseen.utils.SoundManager.get().play("shuriken");
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
                        || ty < 0 || ty >= unseen.utils.Constants.GRID_HEIGHT)
                    break;
                if (levelManager.getMap().getTile(tx, ty) == unseen.map.Tile.WALL)
                    break;
                travelTiles = i;
            }
        }
        if (travelTiles > 0) {
            levelManager.spawnShurikenFlight(px, py, shurikenDx, shurikenDy, travelTiles);
        }

        if (killPos != null) {
            spawnDeathPuff(killPos[0], killPos[1]);
            if (horrorMode) {
                unseen.utils.SoundManager.get().play("blood_splatter", 0.9f);
            }
        }

        processTurnAndApply();
        requestFocusInWindow();
        repaint();
    }

    // -------------------------------------------------------------------------
    // Smoke / flare / death puff
    // -------------------------------------------------------------------------

    @Override
    public void spawnSmoke(int x, int y) {
        levelManager.spawnSmoke(x, y);
    }

    @Override
    public void spawnFlare(int x, int y) {
        levelManager.spawnFlare(x, y);
    }

    @Override
    public void purifyFloor() {
        levelManager.purifyFloor();
        loadAndPlayBackgroundSound(); // Immediately swap to normal music
    }

    @Override
    public void addNoiseFlash(int x, int y) {
        levelManager.addNoiseFlash(x, y);
    }

    @Override
    public void addHolyFlash(int x, int y) {
        levelManager.addHolyFlash(x, y);
    }

    public void spawnDeathPuff(int x, int y) {
        levelManager.spawnDeathPuff(x, y);
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

        if (it instanceof unseen.items.Heart) {
            levelManager.getPlayer().heal(1);
            levelManager.getMap().removeItem(px, py);
            unseen.utils.SoundManager.get().play("item_pickup"); // Or a specific heal sound if available
            showToast("Restored 1 Health!", new Color(255, 100, 100));
        } else {
            boolean pickedUp = levelManager.getPlayer().addItem(it);
            if (!pickedUp) {
                showToast("Too many " + it.getClass().getSimpleName() + "! Max 5.", new Color(255, 110, 110));
                return false;
            }
            levelManager.getMap().removeItem(px, py);
            unseen.utils.SoundManager.get().play("item_pickup");
            showToast("Picked up " + it.getClass().getSimpleName(), new Color(120, 255, 160));
        }

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

    private boolean jumpscareActive = false;
    public boolean isJumpscareActive() { return jumpscareActive; }

    public void setGameState(GameState s) {
        this.state = s;
        if (s == GameState.LOSE) {
            screenShake.trigger(18, 8f);
            if (horrorMode) {
                unseen.utils.SoundManager.get().play("jumpscare");
                jumpscareActive = true;
                // Auto-clear jumpscare after 1.5 seconds
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException e) {}
                    jumpscareActive = false;
                    repaint();
                }).start();
            }
            runStats.setFloorsCleared(levelManager.getFloorNumber() - 1);
            runStats.commitHighScore();
        }
    }

    public GameState getGameState() {
        return state;
    }

    /** Manually trigger a screen shake (frames, magnitude in pixels). */
    public void triggerShake(int frames, float magnitude) {
        screenShake.trigger(frames, magnitude);
    }

    public ScreenShake getScreenShake() {
        return screenShake;
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

    public java.util.List<unseen.ui.gamepanel.ShurikenProjectile> getShurikenProjectiles() {
        return levelManager.getShurikenProjectiles();
    }

    public int getMouseGridX() {
        return targetGridX;
    }
    public int getMouseGridY() {
        return targetGridY;
    }

    public boolean isTargetingShuriken() {
        return targetingShuriken;
    }

    public int getShurikenDx() {
        return shurikenDx;
    }

    public int getShurikenDy() {
        return shurikenDy;
    }

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
