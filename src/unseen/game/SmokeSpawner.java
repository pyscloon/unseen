package unseen.game;

/**
 * Decouples items/entities that need to spawn smoke from the UI layer
 * (GamePanel).
 * GamePanel implements this; items and Player hold a reference to this
 * interface only.
 */
public interface SmokeSpawner {
    void spawnSmoke(int x, int y);

    void spawnFlare(int x, int y);
}
