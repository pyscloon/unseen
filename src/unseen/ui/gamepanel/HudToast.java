package unseen.ui.gamepanel;

/**
 * A brief, auto-fading toast notification that appears at the bottom of the HUD.
 * Created with a message and optional colour tint; fades out over its lifespan.
 */
public class HudToast {

    private final String message;
    private final java.awt.Color color;
    private final long createdAt;
    private final long durationMs;

    /** Default toast -- white text, 2 seconds. */
    public HudToast(String message) {
        this(message, new java.awt.Color(220, 220, 220), 2000L);
    }

    public HudToast(String message, java.awt.Color color) {
        this(message, color, 2000L);
    }

    public HudToast(String message, java.awt.Color color, long durationMs) {
        this.message    = message;
        this.color      = color;
        this.createdAt  = System.currentTimeMillis();
        this.durationMs = durationMs;
    }

    public String getMessage() { return message; }
    public java.awt.Color getColor() { return color; }

    /** Returns 1.0 -> 0.0 as the toast ages. */
    public float getAlpha() {
        long age = System.currentTimeMillis() - createdAt;
        if (age >= durationMs) return 0f;
        // First 60% of life: full opacity. Last 40%: linear fade.
        float progress = (float) age / durationMs;
        if (progress < 0.6f) return 1f;
        return 1f - (progress - 0.6f) / 0.4f;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt >= durationMs;
    }
}
