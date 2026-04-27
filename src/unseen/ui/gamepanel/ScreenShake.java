package unseen.ui.gamepanel;

import java.util.Random;

/**
 * Lightweight screen-shake effect.
 *
 * Call {@link #trigger(int, float)} when something impactful happens
 * (e.g. player spotted). Each frame, call {@link #update()} then read
 * {@link #getOffsetX()} / {@link #getOffsetY()} and translate the
 * Graphics2D context before drawing.
 */
public class ScreenShake {

    private static final Random RNG = new Random();

    private int   framesLeft = 0;
    private float magnitude  = 0f;
    private float offsetX    = 0f;
    private float offsetY    = 0f;

    /**
     * Starts (or reinforces) a shake.
     *
     * @param frames    how many frames the shake lasts
     * @param magnitude maximum pixel displacement
     */
    public void trigger(int frames, float magnitude) {
        this.framesLeft = Math.max(this.framesLeft, frames);
        this.magnitude  = Math.max(this.magnitude,  magnitude);
    }

    /** Must be called once per render frame. */
    public void update() {
        if (framesLeft <= 0) {
            offsetX = 0f;
            offsetY = 0f;
            return;
        }
        // Exponential decay so the shake eases out smoothly
        float decay = framesLeft / (float) Math.max(framesLeft, 1);
        float m = magnitude * decay;
        offsetX = (RNG.nextFloat() * 2f - 1f) * m;
        offsetY = (RNG.nextFloat() * 2f - 1f) * m;
        framesLeft--;
        if (framesLeft <= 0) magnitude = 0f;
    }

    public boolean isActive() { return framesLeft > 0; }
    public float   getOffsetX() { return offsetX; }
    public float   getOffsetY() { return offsetY; }
}
