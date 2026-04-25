package unseen.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.net.URL;

/**
 * Singleton that loads every game sprite exactly once.
 * All classes should reference images through AssetLoader.get()
 * rather than performing their own ImageIO.read() calls.
 */
public class AssetLoader {

    private static AssetLoader instance;

    public final Image wall;
    public final Image floor;
    public final Image torch;
    public final Image noiseMaker;
    public final Image smokeBomb;
    public final Image lantern;
    public final Image nextFloor;
    public final Image shuriken; // NEW

    public final Image hero;

    public final Image enemyUp;
    public final Image enemyDown;
    public final Image enemyLeft;
    public final Image enemyRight;
    public final Image enemyBase;
    public final Image sentry;
    public final Image patrol;

    private AssetLoader() {
        wall       = load("unseen/assets/wall.png");
        floor      = load("unseen/assets/tile.png");
        torch      = load("unseen/assets/torch.png");
        noiseMaker = load("unseen/assets/noise.png");
        smokeBomb  = load("unseen/assets/smoke.png");
        lantern    = load("unseen/assets/lantern.png");
        nextFloor  = load("unseen/assets/next_floor.png");
        shuriken   = load("unseen/assets/shuriken.png");

        hero = load("unseen/assets/hero.png");

        enemyUp    = load("unseen/assets/up-enemy.png");
        enemyDown  = load("unseen/assets/down-enemy.png");
        enemyLeft  = load("unseen/assets/left-enemy.png");
        enemyRight = load("unseen/assets/right-enemy.png");
        enemyBase  = load("unseen/assets/enemy.png");
        sentry     = load("unseen/assets/sentry.png");
        patrol     = load("unseen/assets/patrol.png");
    }

    private Image load(String path) {
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(path);
            if (url != null) {
                return ImageIO.read(url);
            }
            System.out.println("Asset not found: " + path);
        } catch (Exception e) {
            System.out.println("Failed to load asset '" + path + "': " + e.getMessage());
        }
        return null;
    }

    public static AssetLoader get() {
        if (instance == null) {
            instance = new AssetLoader();
        }
        return instance;
    }
}