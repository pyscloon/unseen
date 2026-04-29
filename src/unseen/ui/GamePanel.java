package unseen.ui;

import unseen.entities.Enemy;
import unseen.entities.Player;
import unseen.game.*;
import unseen.items.GrapplingHook;
import unseen.items.Item;
import unseen.items.Shuriken;
import unseen.map.Map;
import unseen.ui.gamepanel.*;
import unseen.utils.Constants;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable, SmokeSpawner {
    private static final int HORROR_JUMPSCARE_TURN_THRESHOLD = 60;
    private static final String[] INTRO_NARRATION = {
            "Beneath Room 205 of the Engineering Building, below the old software engineering room, there waits a dungeon that should never have been found.",
            "Then a virus spread through the buried halls. Stone soured. Shadows learned to move. And from the dark came monsters, as if broken code itself had grown teeth and hunger.",
            "The first to descend were Lon, Ron, and Dom. They entered as heroes. Whether they died in the deep or vanished into it, no one can say. Only their relics remain, waiting on the floors for the next hand brave enough to claim them.",
            "Now that burden passes to you. One day, chasing adventure, you found the hidden path beneath Room 205 and chose to challenge the legend yourself."
    };
    private static final int INTRO_CHAR_REVEAL_MS = 65;
    private static final int INTRO_PARAGRAPH_PAUSE_MS = 900;
    private static final int INTRO_PARAGRAPH_PAUSE_TICKS = Math.max(1, INTRO_PARAGRAPH_PAUSE_MS / INTRO_CHAR_REVEAL_MS);

    private javax.sound.sampled.Clip backgroundClip;
    private javax.sound.sampled.Clip narratorClip;

    private Thread gameThread;
    private GameState state = GameState.INTRO;

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
    private int grapplePathHits = 0;
    private boolean grappleHitWall = false;
    private unseen.items.Item grappleUsedItem = null;

    private final LevelManager levelManager;
    private final GameRenderer renderer;
    private final TutorialManager tutorial = new TutorialManager();
    private final ScreenShake screenShake = new ScreenShake();
    private final RunStats runStats = new RunStats();

    private final QuestManager questManager = new QuestManager();
    private final Random rewardRandom = new Random();
    private final List<RewardChoice> floorRewardChoices = new ArrayList<>();
    private String questNotificationText = null;
    private long questNotificationUntil = 0L;
    private boolean achievementsOpen = false;

    private boolean roundQuestHudVisible = false;
    private int horrorModeTurnsThisRun = 0;
    private boolean timedHorrorJumpscareTriggered = false;


    /** HUD toast queue -- most recent at the end. */
    private final List<HudToast> toasts = new ArrayList<>();

    /** Millisecond timestamp of the last hit -- used for red vignette flash. */
    private long lastHitTime = 0;
    private volatile long freezeUntil = 0;

    private boolean horrorMode = false;
    private String currentNoteLore = null;
    private long introRevealStartMs;
    private int introForcedVisibleTicks = -1;
    private final int introTotalTicks = countIntroTicks(INTRO_NARRATION);
    private long introFullyRevealedMs = -1;

    public boolean isHorrorMode() {
        return horrorMode;
    }

    public String getCurrentNoteLore() {
        return currentNoteLore;
    }

    public void setCurrentNoteLore(String lore) {
        if (this.currentNoteLore == null && lore != null) {
            unseen.utils.SoundManager.get().play("paper", 0.7f);
        }
        this.currentNoteLore = lore;
    }

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

    public QuestManager getQuestManager() {
        return questManager;
    }

    public List<RewardChoice> getFloorRewardChoices() {
        return floorRewardChoices;
    }

    public String getQuestNotificationText() {
        if (questNotificationText == null || System.currentTimeMillis() > questNotificationUntil) {
            return null;
        }
        return questNotificationText;
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
                if (state == GameState.INTRO
                        || state == GameState.MENU || tutorial.isActive()
                        || targetingNoiseMaker || targetingFlare || targetingGrapplingHook)
                    repaint();
            }
        });

        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (state == GameState.INTRO) {
                    playUiClick();
                    advanceIntro();
                    requestFocusInWindow();
                    return;
                }

                if (state == GameState.REWARD_CHOICE) {
                    int choice = getRewardChoiceAt(e.getX(), e.getY());
                    if (choice >= 0) {
                        playUiClick();
                        chooseFloorReward(choice);
                    }
                    requestFocusInWindow();
                    return;
                }


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

                if (state == GameState.MENU) {
                    if (achievementsOpen) {
                        playUiClick();
                        closeAchievements();
                        requestFocusInWindow();
                        return;
                    }

                    GameRenderer.MenuAction action = renderer.getMenuActionAt(e.getX(), e.getY(), getWidth(), getHeight());
                    switch (action) {
                        case START:
                            playUiClick();
                            startFromMenu();
                            return;
                        case TUTORIAL:
                            playUiClick();
                            tutorial.reset();
                            repaint();
                            return;
                        case ACHIEVEMENTS:
                            playUiClick();
                            openAchievements();
                            return;
                        case TOGGLE_HORROR:
                            playHorrorToggleClick(!isHorrorMode());
                            setHorrorMode(!isHorrorMode());
                            repaint();
                            return;
                        case QUIT:
                            playUiClick();
                            System.exit(0);
                            return;
                        default:
                            break;
                    }
                }


                // Noise/Flare targeting clicks during gameplay
                if (targetingNoiseMaker) {
                    playUiClick();
                    confirmNoiseTarget(targetGridX, targetGridY);
                } else if (targetingFlare) {
                    playUiClick();
                    confirmFlareTarget(targetGridX, targetGridY);
                } else if (targetingGrapplingHook) {
                    playUiClick();
                    confirmGrapplingHookTarget(targetGridX, targetGridY);
                }
            }
        });

        levelManager = new LevelManager();
        levelManager.setPanel(this);
        renderer = new GameRenderer(this, levelManager);

        questManager.resetRun();
        floorRewardChoices.clear();
        questNotificationText = null;


        levelManager.setupGame();
        loadAndPlayBackgroundSound();
        resetIntro();
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
            if (currentTrackIndex >= activePlaylist.length)
                currentTrackIndex = 0;

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

    public void stopBackgroundSound() {
        if (backgroundClip != null) {
            backgroundClip.stop();
        }
    }

    private void playNarration() {
        stopNarration();
        try {
            java.net.URL soundURL = getClass().getClassLoader().getResource("unseen/assets/sound/narrator.wav");
            if (soundURL == null) {
                // Fallback to mp3 if wav is missing
                soundURL = getClass().getClassLoader().getResource("unseen/assets/sound/narrator.mp3");
            }

            if (soundURL != null) {
                javax.sound.sampled.AudioInputStream audioIn = javax.sound.sampled.AudioSystem
                        .getAudioInputStream(soundURL);
                narratorClip = javax.sound.sampled.AudioSystem.getClip();
                narratorClip.open(audioIn);
                narratorClip.start();
            }
        } catch (Exception e) {
            System.err.println("Narration Error: " + e.getMessage());
        }
    }

    private void stopNarration() {
        if (narratorClip != null) {
            narratorClip.stop();
            narratorClip.close();
            narratorClip = null;
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

    public void playUiClick() {
        unseen.utils.SoundManager.get().play("ui_click", 0.75f);
    }

    public void playHorrorToggleClick(boolean turningOn) {
        if (turningOn) {
            unseen.utils.SoundManager.get().play("horror_click", 0.85f);
        } else {
            playUiClick();
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
        questManager.resetRun();
        resetTimedHorrorJumpscare();
        levelManager.setupGame();
        floorRewardChoices.clear();
        questNotificationText = null;
        achievementsOpen = false;
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
        questManager.resetRun();
        resetTimedHorrorJumpscare();
        floorRewardChoices.clear();
        questNotificationText = null;
        achievementsOpen = false;
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
        questManager.resetRun();
        resetTimedHorrorJumpscare();
        floorRewardChoices.clear();
        questNotificationText = null;
        achievementsOpen = false;
        levelManager.setupGame();
        setGameState(GameState.PLAYING);
        requestFocusInWindow();
        repaint();
    }

    public void nextFloor() {
        unseen.utils.SoundManager.get().play("ladder");
        runStats.setFloorsCleared(levelManager.getFloorNumber());
        levelManager.nextFloor();
        questManager.startQuestForFloor(levelManager.getFloorNumber());
        floorRewardChoices.clear();
        loadAndPlayBackgroundSound(); // Restore horror music if applicable
        setGameState(GameState.PLAYING);
        requestFocusInWindow();
        repaint();
    }


    public void startGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    private void resetIntro() {
        introRevealStartMs = System.currentTimeMillis();
        introForcedVisibleTicks = -1;
        introFullyRevealedMs = -1;
        playNarration();
    }

    private static int countIntroTicks(String[] paragraphs) {
        int total = 0;
        int visibleParagraphs = 0;
        for (String paragraph : paragraphs) {
            if (paragraph != null && !paragraph.isEmpty()) {
                total += paragraph.length();
                visibleParagraphs++;
            }
        }
        if (visibleParagraphs > 1) {
            total += (visibleParagraphs - 1) * INTRO_PARAGRAPH_PAUSE_TICKS;
        }
        return total;
    }

    public String[] getIntroNarration() {
        return INTRO_NARRATION;
    }

    public int getIntroVisibleTicks() {
        if (introForcedVisibleTicks >= 0) {
            return introForcedVisibleTicks;
        }
        long elapsed = Math.max(0L, System.currentTimeMillis() - introRevealStartMs);
        int visible = 1 + (int) (elapsed / INTRO_CHAR_REVEAL_MS);
        return Math.min(introTotalTicks, visible);
    }

    public int getIntroTotalTicks() {
        return introTotalTicks;
    }

    public int getIntroParagraphPauseTicks() {
        return INTRO_PARAGRAPH_PAUSE_TICKS;
    }

    public boolean isIntroFullyRevealed() {
        return getIntroVisibleTicks() >= introTotalTicks;
    }

    public void advanceIntro() {
        if (state != GameState.INTRO) {
            return;
        }
        if (!isIntroFullyRevealed()) {
            introForcedVisibleTicks = introTotalTicks;
        } else {
            startIntroVideo();
        }
        repaint();
    }

    private void startIntroVideo() {
        // Since we are skipping the video, we transition straight to the menu
        state = GameState.MENU;
        stopBackgroundSound();
        stopNarration();
        loadAndPlayBackgroundSound();
        requestFocusInWindow();
        repaint();
    }

    public void skipIntroToMenu() {
        if (state != GameState.INTRO) {
            return;
        }
        stopNarration();
        introForcedVisibleTicks = introTotalTicks;
        state = GameState.MENU;
        requestFocusInWindow();
        repaint();
    }

    @Override
    public void run() {
        // ~30 FPS - smooth enough for screen-shake and HUD animations
        final long TARGET_MS = 33L;
        while (gameThread != null) {
            long start = System.currentTimeMillis();

            boolean frozen = System.currentTimeMillis() < freezeUntil;
            if (!frozen && state == GameState.PLAYING && horrorMode) {
                levelManager.updateVisibility();
            }

            if (!frozen && grappling) {
                updateGrappling();
            }

            if (!frozen && state == GameState.PLAYING) {
                updateShurikenImpacts();
            }

            if (state == GameState.INTRO && isIntroFullyRevealed()) {
                boolean narrationDone = (narratorClip == null || !narratorClip.isRunning());
                if (narrationDone) {
                    if (introFullyRevealedMs == -1) {
                        introFullyRevealedMs = System.currentTimeMillis();
                    } else if (System.currentTimeMillis() - introFullyRevealedMs > 1500) {
                        startIntroVideo();
                    }
                }
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
        updateTimedHorrorJumpscare(result.state);
        runStats.incrementKills(result.killsThisTurn);
        runStats.setFloorsCleared(levelManager.getFloorNumber() - 1);

        checkQuestCompletion(questManager.recordTurn());
        checkQuestCompletion(questManager.recordKills(result.killsThisTurn));
        drainAchievementToasts();

        if (result.playerHit) {
            handlePlayerDamaged();
        }

        if (result.state == GameState.WIN) {
            boolean clearedInHorror = horrorMode && !levelManager.isFloorPurified();
            checkQuestCompletion(questManager.recordFloorCleared(levelManager.getFloorNumber(), clearedInHorror));
            drainAchievementToasts();
            beginPostFloorRewards();
        } else {
            setGameState(result.state);
        }


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

    public boolean isValidGrapplingHookTarget(int gx, int gy) {
        java.util.List<unseen.items.Item> inv = levelManager.getPlayer().getInventory();
        for (unseen.items.Item item : inv) {
            if (item instanceof unseen.items.GrapplingHook) {
                unseen.items.GrapplingHook hook = (unseen.items.GrapplingHook) item;
                return hook.isValidWallTarget(levelManager.getPlayer(), levelManager.getMap(), gx, gy)
                        && hook.findLandingSpot(levelManager.getPlayer(), levelManager.getMap(),
                                levelManager.getEnemies(), gx, gy) != null;
            }
        }
        return false;
    }

    public int[] getGrappleLandingPreview(int gx, int gy) {
        java.util.List<unseen.items.Item> inv = levelManager.getPlayer().getInventory();
        for (unseen.items.Item item : inv) {
            if (item instanceof unseen.items.GrapplingHook) {
                unseen.items.GrapplingHook hook = (unseen.items.GrapplingHook) item;
                return hook.findLandingSpot(levelManager.getPlayer(), levelManager.getMap(),
                        levelManager.getEnemies(), gx, gy);
            }
        }
        return null;
    }

    private void confirmNoiseTarget(int gx, int gy) {
        if (gx < 0 || gy < 0 || gx >= Constants.GRID_WIDTH || gy >= Constants.GRID_HEIGHT) {
            showToast("Target is outside the floor.", new Color(220, 120, 90));
            return;
        }
        if (!levelManager.getMap().isPassable(gx, gy)) {
            showToast("Noise makers need an open tile.", new Color(220, 120, 90));
            return;
        }

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
        levelManager.addTileEffect(gx, gy, TileEffect.Kind.ALERT);
        targetingNoiseMaker = false;

        processTurnAndApply();
        requestFocusInWindow();
        repaint();
    }

    private void confirmFlareTarget(int gx, int gy) {
        if (gx < 0 || gy < 0 || gx >= Constants.GRID_WIDTH || gy >= Constants.GRID_HEIGHT) {
            showToast("Target is outside the floor.", new Color(220, 120, 90));
            return;
        }
        if (!levelManager.getMap().isPassable(gx, gy)) {
            showToast("Flares need an open tile.", new Color(220, 120, 90));
            return;
        }

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
        levelManager.addTileEffect(gx, gy, TileEffect.Kind.PICKUP);
        targetingFlare = false;

        processTurnAndApply();
        requestFocusInWindow();
        repaint();
    }

    private void confirmGrapplingHookTarget(int gx, int gy) {
        if (gx < 0 || gy < 0 || gx >= Constants.GRID_WIDTH || gy >= Constants.GRID_HEIGHT) {
            showToast("Target is outside the floor.", new Color(220, 120, 90));
            return;
        }

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

        int[] landing = hook.findLandingSpot(levelManager.getPlayer(), levelManager.getMap(), levelManager.getEnemies(),
                gx, gy);
        if (landing == null) {
            showToast("Hook needs a reachable wall and open landing tile.", new Color(220, 120, 90));
            repaint();
            return;
        }

        unseen.utils.SoundManager.get().play("grapple", 0.8f);

        grappling = true;
        grappleProgress = 0f;
        grappleStartX = levelManager.getPlayer().getX();
        grappleStartY = levelManager.getPlayer().getY();
        grappleEndX = landing[0];
        grappleEndY = landing[1];
        grappleWallX = gx;
        grappleWallY = gy;
        grapplePathHits = hook.countEnemiesInPath(levelManager.getPlayer(), levelManager.getMap(),
                levelManager.getEnemies(), gx, gy);
        grappleHitWall = false;
        grappleUsedItem = inv.remove(idx);
        targetingGrapplingHook = false;
        repaint();
    }

    private void updateGrappling() {
        float oldProgress = grappleProgress;
        grappleProgress += 0.15f; // Animation speed

        if (oldProgress < 0.4f && grappleProgress >= 0.4f && !grappleHitWall) {
            grappleHitWall = true;
            triggerShake(10, 3f);
            triggerFreeze(50);
        }

        if (grappleProgress >= 1.0f) {
            grappleProgress = 1.0f;
            grappling = false;

            // Apply the actual movement now
            levelManager.getPlayer().setPosition(grappleEndX, grappleEndY);
            levelManager.addTileEffect(grappleEndX, grappleEndY, TileEffect.Kind.PICKUP);
            // Update facing
            if (grappleEndX < grappleStartX)
                levelManager.getPlayer().setFacing(Player.Facing.LEFT);
            else if (grappleEndX > grappleStartX)
                levelManager.getPlayer().setFacing(Player.Facing.RIGHT);

            if (grapplePathHits > 0) {
                applyDirectPlayerDamage();
            }

            showToast("Grapple zip!", new Color(150, 220, 255));
            grapplePathHits = 0;
            if (state != GameState.LOSE) {
                processTurnAndApply();
            }
            requestFocusInWindow();
        }
        repaint();
    }

    private void applyDirectPlayerDamage() {
        if (!levelManager.getPlayer().takeDamage()) {
            return;
        }

        handlePlayerDamaged();
    }

    private void handlePlayerDamaged() {
        questManager.recordHit();
        lastHitTime = System.currentTimeMillis();
        levelManager.addTileEffect(levelManager.getPlayer().getX(), levelManager.getPlayer().getY(),
                TileEffect.Kind.DAMAGE);
        if (horrorMode) {
            unseen.utils.SoundManager.get().play("blood_splatter", 0.8f);
        } else {
            unseen.utils.SoundManager.get().play("player_hit");
        }

        int hp = levelManager.getPlayer().getHealth();
        if (hp > 0) {
            screenShake.trigger(12, 6f);
            showToast("HIT! " + hp + " HP remaining", new Color(255, 80, 60));
        } else {
            setGameState(GameState.LOSE);
        }
    }

    public boolean isGrappling() {
        return grappling;
    }

    public float getGrappleProgress() {
        return grappleProgress;
    }

    public int getGrappleStartX() {
        return grappleStartX;
    }

    public int getGrappleStartY() {
        return grappleStartY;
    }

    public int getGrappleWallX() {
        return grappleWallX;
    }

    public int getGrappleWallY() {
        return grappleWallY;
    }

    public int getGrappleEndX() {
        return grappleEndX;
    }

    public int getGrappleEndY() {
        return grappleEndY;
    }

    public void confirmShurikenThrow() {
        if (!levelManager.getShurikenProjectiles().isEmpty()) {
            return;
        }

        List<Item> inv = levelManager.getPlayer().getInventory();
        int idx = -1;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i) instanceof Shuriken) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            targetingShuriken = false;
            return;
        }

        int px = levelManager.getPlayer().getX();
        int py = levelManager.getPlayer().getY();
        int travelTiles;
        boolean hitWall = false;
        Enemy targetEnemy = null;
        int targetX = -1;
        int targetY = -1;
        travelTiles = 0;
        for (int i = 1; i <= unseen.items.Shuriken.RANGE; i++) {
            int tx = px + shurikenDx * i;
            int ty = py + shurikenDy * i;
            if (tx < 0 || tx >= unseen.utils.Constants.GRID_WIDTH
                    || ty < 0 || ty >= unseen.utils.Constants.GRID_HEIGHT) {
                break;
            }
            if (levelManager.getMap().getTile(tx, ty) == unseen.map.Tile.WALL) {
                hitWall = true;
                break;
            }
            travelTiles = i;
            for (Enemy enemy : levelManager.getEnemies()) {
                if (enemy.isAlive() && enemy.getX() == tx && enemy.getY() == ty) {
                    targetEnemy = enemy;
                    targetX = tx;
                    targetY = ty;
                    break;
                }
            }
            if (targetEnemy != null) {
                break;
            }
        }
        if (travelTiles <= 0) {
            showToast("Throw blocked.", new Color(220, 120, 90));
            repaint();
            return;
        }

        unseen.utils.SoundManager.get().play("shuriken");
        inv.remove(idx);
        targetingShuriken = false;

        if (travelTiles > 0) {
            levelManager.spawnShurikenFlight(px, py, shurikenDx, shurikenDy, travelTiles,
                    targetEnemy, targetX, targetY, hitWall);
        }

        requestFocusInWindow();
        repaint();
    }

    private void updateShurikenImpacts() {
        java.util.List<ShurikenProjectile> projectiles = levelManager.getShurikenProjectiles();
        if (projectiles.isEmpty()) {
            return;
        }

        java.util.Iterator<ShurikenProjectile> it = projectiles.iterator();
        while (it.hasNext()) {
            ShurikenProjectile projectile = it.next();
            if (!projectile.isDone()) {
                continue;
            }

            it.remove();
            resolveShurikenImpact(projectile);
            processTurnAndApply();
            repaint();
            return;
        }
    }

    private void resolveShurikenImpact(ShurikenProjectile projectile) {
        Enemy target = projectile.getTargetEnemy();
        if (target != null && target.isAlive()) {
            target.die();
            if (!target.isAlive()) {
                int tx = projectile.getTargetX();
                int ty = projectile.getTargetY();
                Enemy.EnemyType killedType = projectile.getTargetEnemyType();
                levelManager.getMap().setDecal(tx, ty,
                        killedType == Enemy.EnemyType.PATROL
                                ? unseen.map.DecalType.DEAD_BONES
                                : (killedType == Enemy.EnemyType.HUNTER
                                ? unseen.map.DecalType.PUDDLE
                                : unseen.map.DecalType.BLOOD_TILE));
                String killSound = killedType == Enemy.EnemyType.PATROL ? "bone_break" : "splash";
                unseen.utils.SoundManager.get().play(killSound, 0.9f);
            }
        }

        if (target != null || projectile.hitWall()) {
            triggerShake(10, 3f);
            triggerFreeze(50);
        }
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
        levelManager.addTileEffect(levelManager.getPlayer().getX(), levelManager.getPlayer().getY(),
                TileEffect.Kind.PURIFY);
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
            levelManager.addTileEffect(px, py, TileEffect.Kind.PICKUP);
        } else {
            boolean pickedUp = levelManager.getPlayer().addItem(it);
            if (!pickedUp) {
                showToast("Too many " + itemDisplayName(it) + "! Max 5.", new Color(255, 110, 110));
                return false;
            }
            levelManager.getMap().removeItem(px, py);
            unseen.utils.SoundManager.get().play("item_pickup");
            showToast("Picked up " + itemDisplayName(it), new Color(120, 255, 160));
            levelManager.addTileEffect(px, py, TileEffect.Kind.PICKUP);
        }
        checkQuestCompletion(questManager.recordPickup());
        drainAchievementToasts();
        processTurnAndApply();

        levelManager.updateVisibility();
        return true;
    }

    public boolean interactWithBarrel() {
        Player player = levelManager.getPlayer();

        if (player.isHiddenInBarrel()) {
            player.setHiddenInBarrel(false);
            unseen.utils.SoundManager.get().play("ladder", 0.6f);
            showToast("Left the barrel.", new Color(180, 190, 210));
            levelManager.updateVisibility();
            repaint();
            return true;
        }

        int[] barrel = getNearbyBarrel();
        if (barrel == null) {
            return false;
        }

        for (Enemy enemy : levelManager.getEnemies()) {
            if (enemy.isAlive() && enemy.getX() == barrel[0] && enemy.getY() == barrel[1]) {
                showToast("Something is blocking the barrel.", new Color(220, 120, 90));
                return true;
            }
        }

        player.setPosition(barrel[0], barrel[1]);
        player.setHiddenInBarrel(true);
        unseen.utils.SoundManager.get().play("ladder", 0.6f);
        showToast("Hidden in barrel.", new Color(190, 210, 180));
        processTurnAndApply();
        levelManager.updateVisibility();
        repaint();
        return true;
    }

    private int[] getNearbyBarrel() {
        Player player = levelManager.getPlayer();
        int px = player.getX();
        int py = player.getY();
        int[] best = null;
        int bestDist = Integer.MAX_VALUE;

        for (int[] barrel : levelManager.getBarrels()) {
            int dist = Math.abs(barrel[0] - px) + Math.abs(barrel[1] - py);
            if (dist <= 1 && dist < bestDist) {
                best = barrel;
                bestDist = dist;
            }
        }

        return best;
    }

    private String itemDisplayName(Item item) {
        if (item instanceof unseen.items.NoiseMaker)
            return "Noise Maker";
        if (item instanceof unseen.items.SmokeBomb)
            return "Smoke Bomb";
        if (item instanceof unseen.items.Flare)
            return "Flare";
        if (item instanceof unseen.items.Shuriken)
            return "Shuriken";
        if (item instanceof unseen.items.GrapplingHook)
            return "Grappling Hook";
        if (item instanceof unseen.items.Cross)
            return "Holy Cross";
        if (item instanceof unseen.items.Heart)
            return "Heart";
        return item.getClass().getSimpleName();
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



    public boolean isAchievementsOpen() {
        return achievementsOpen;
    }

    public void openAchievements() {
        achievementsOpen = true;
        repaint();
    }

    public void closeAchievements() {
        achievementsOpen = false;
        repaint();
    }

    private boolean jumpscareActive = false;
    private boolean redJumpscareActive = false;

    public boolean isJumpscareActive() {
        return jumpscareActive;
    }

    public boolean isRedJumpscareActive() {
        return redJumpscareActive;
    }

    private void resetTimedHorrorJumpscare() {
        horrorModeTurnsThisRun = 0;
        timedHorrorJumpscareTriggered = false;
        redJumpscareActive = false;
        jumpscareActive = false;
    }

    private void updateTimedHorrorJumpscare(GameState turnState) {
        if (timedHorrorJumpscareTriggered
                || turnState != GameState.PLAYING
                || !horrorMode
                || levelManager.isFloorPurified()) {
            return;
        }

        horrorModeTurnsThisRun++;
        if (horrorModeTurnsThisRun >= HORROR_JUMPSCARE_TURN_THRESHOLD) {
            triggerTimedHorrorJumpscare();
        }
    }

    private void triggerTimedHorrorJumpscare() {
        timedHorrorJumpscareTriggered = true;
        redJumpscareActive = true;
        jumpscareActive = true;
        triggerShake(28, 7f);
        triggerFreeze(350);
        unseen.utils.SoundManager.get().play("jumpscare");

        new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
            }
            redJumpscareActive = false;
            jumpscareActive = false;
            repaint();
        }).start();
    }

    public void setGameState(GameState s) {
        this.state = s;
        if (s == GameState.LOSE) {
            screenShake.trigger(18, 8f);
            if (horrorMode) {
                unseen.utils.SoundManager.get().play("jumpscare");
                redJumpscareActive = false;
                jumpscareActive = true;
                // Auto-clear jumpscare after 1.5 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                    }
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

    public void triggerFreeze(long ms) {
        freezeUntil = Math.max(freezeUntil, System.currentTimeMillis() + ms);
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

    public void addTileEffect(int x, int y, TileEffect.Kind kind) {
        levelManager.addTileEffect(x, y, kind);
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

    public boolean isShurikenInFlight() {
        return !levelManager.getShurikenProjectiles().isEmpty();
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
        if (dx == 0 && dy == 0) {
            return;
        }
        shurikenDx = Integer.signum(dx);
        shurikenDy = Integer.signum(dy);
        repaint();
    }

    public void angleShurikenDirection(int dx, int dy) {
        if (dx != 0) {
            int nextDx = Integer.signum(dx);
            int nextDy = shurikenDy != 0 ? shurikenDy : 0;
            setShurikenDirection(nextDx, nextDy);
        } else if (dy != 0) {
            int nextDx = shurikenDx != 0 ? shurikenDx : 0;
            int nextDy = Integer.signum(dy);
            setShurikenDirection(nextDx, nextDy);
        }
        repaint();
    }

    private void checkQuestCompletion(boolean completed) {
        if (!completed) {
            return;
        }

        QuestManager.Quest quest = questManager.getActiveQuest();
        Item reward = createRandomRewardItem();
        String rewardName = itemDisplayName(reward);
        boolean granted = grantRewardItem(reward);

        String questName = quest != null ? quest.getName() : "Quest";
        String message = granted
                ? "QUEST COMPLETE: " + questName + "  + " + rewardName
                : "QUEST COMPLETE: " + questName + "  Inventory full";
        showQuestNotification(message);

        if (granted) {
            showToast("Quest reward: " + rewardName, new Color(255, 220, 120));
        } else {
            showToast("Quest complete, but your pack is full.", new Color(255, 130, 90));
        }
    }

    private void showQuestNotification(String message) {
        questNotificationText = message;
        questNotificationUntil = System.currentTimeMillis() + 3200L;
        unseen.utils.SoundManager.get().play("item_pickup", 0.75f);
    }

    private void beginPostFloorRewards() {
        generateFloorRewardChoices();
        setGameState(GameState.REWARD_CHOICE);
        requestFocusInWindow();
        repaint();
    }

    private void generateFloorRewardChoices() {
        floorRewardChoices.clear();

        java.util.List<Item> pool = new java.util.ArrayList<>();
        pool.add(new unseen.items.NoiseMaker());
        pool.add(new unseen.items.SmokeBomb());
        pool.add(new unseen.items.Flare());
        pool.add(new unseen.items.Shuriken());
        pool.add(new unseen.items.GrapplingHook());

        if (horrorMode) {
            pool.add(new unseen.items.Cross());
        }

        java.util.Collections.shuffle(pool, rewardRandom);

        for (int i = 0; i < 3 && i < pool.size(); i++) {
            Item item = pool.get(i);
            floorRewardChoices.add(new RewardChoice(itemDisplayName(item), item));
        }
    }

    public void chooseFloorReward(int index) {
        if (index < 0 || index >= floorRewardChoices.size()) {
            return;
        }
        RewardChoice choice = floorRewardChoices.get(index);
        if (grantRewardItem(choice.getItem())) {
            showToast("Reward claimed: " + choice.getName(), new Color(255, 220, 120));
        } else {
            showToast("Could not carry " + choice.getName() + ".", new Color(255, 130, 90));
        }
        nextFloor();
    }

    private int getRewardChoiceAt(int mouseX, int mouseY) {
        int cardW = 180;
        int cardH = 92;
        int gap = 18;
        int totalW = cardW * 3 + gap * 2;
        int startX = (getWidth() - totalW) / 2;
        int y = getHeight() / 2 + 34;
        for (int i = 0; i < 3; i++) {
            java.awt.Rectangle bounds = new java.awt.Rectangle(startX + i * (cardW + gap), y, cardW, cardH);
            if (bounds.contains(mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }

    private boolean grantRewardItem(Item item) {
        return levelManager.getPlayer().addItem(item);
    }

    private Item createRandomRewardItem() {
        int choices = horrorMode ? 6 : 5;
        switch (rewardRandom.nextInt(choices)) {
            case 0:
                return new unseen.items.NoiseMaker();
            case 1:
                return new unseen.items.SmokeBomb();
            case 2:
                return new unseen.items.Flare();
            case 3:
                return new unseen.items.Shuriken();
            case 4:
                return new unseen.items.GrapplingHook();
            default:
                return new unseen.items.Cross();
        }
    }

    public void triggerFakeExit() {
        questManager.recordFakeExit();
        drainAchievementToasts();
        showToast("It's a fake ladder!", new Color(255, 150, 70));
        triggerShake(14, 5f);
        levelManager.getPlayer().setTrapped(2);
        levelManager.addTileEffect(levelManager.getPlayer().getX(), levelManager.getPlayer().getY(),
                unseen.ui.gamepanel.TileEffect.Kind.ALERT);
    }

    private void drainAchievementToasts() {
        for (String message : questManager.drainAchievementToasts()) {
            showToast(message, new Color(255, 220, 120));
        }
    }


    public boolean isRoundQuestHudVisible() {
        return roundQuestHudVisible;
    }

    public void toggleRoundQuestHud() {
        roundQuestHudVisible = !roundQuestHudVisible;
        repaint();
    }




}
