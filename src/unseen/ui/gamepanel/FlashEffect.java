package unseen.ui.gamepanel;

/** Ripple/pulse effect drawn at the NoiseMaker decoy tile for a few turns. */
public class FlashEffect {
    private final int x, y;
    private int countdown;

    public FlashEffect(int x, int y, int countdown) {
        this.x = x;
        this.y = y;
        this.countdown = countdown;
    }

    public int getX() { return x; }

    public int getY() { return y; }

    public int getCountdown() { return countdown; }

    public void decrement() { countdown--; }

    public boolean isExpired() { return countdown <= 0; }
}
