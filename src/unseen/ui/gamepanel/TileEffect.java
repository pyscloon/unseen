package unseen.ui.gamepanel;

import java.awt.Color;

/** Short-lived tile pulse used for small gameplay feedback moments. */
public class TileEffect {
    public enum Kind {
        PICKUP,
        ALERT,
        DAMAGE,
        EXIT,
        PURIFY,
        TRAP
    }

    private final int x;
    private final int y;
    private final Kind kind;
    private final long startMs;
    private final int durationMs;

    public TileEffect(int x, int y, Kind kind) {
        this.x = x;
        this.y = y;
        this.kind = kind;
        this.startMs = System.currentTimeMillis();
        this.durationMs = durationFor(kind);
    }

    private static int durationFor(Kind kind) {
        switch (kind) {
            case PURIFY:
                return 1200;
            case EXIT:
                return 900;
            case DAMAGE:
            case TRAP:
                return 650;
            default:
                return 750;
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public Kind getKind() { return kind; }

    public float getProgress() {
        return Math.min(1f, (System.currentTimeMillis() - startMs) / (float) durationMs);
    }

    public boolean isExpired() {
        return getProgress() >= 1f;
    }

    public Color getColor(int alpha) {
        switch (kind) {
            case PICKUP:
                return new Color(120, 255, 160, alpha);
            case ALERT:
                return new Color(255, 70, 55, alpha);
            case DAMAGE:
                return new Color(255, 40, 35, alpha);
            case EXIT:
                return new Color(130, 255, 120, alpha);
            case PURIFY:
                return new Color(255, 245, 120, alpha);
            case TRAP:
                return new Color(255, 130, 40, alpha);
            default:
                return new Color(255, 255, 255, alpha);
        }
    }
}
