package unseen.ui.gamepanel;

/** Ripple/pulse effect drawn at the NoiseMaker decoy tile or for Holy Cross purification. */
public class FlashEffect {
    private final int x, y;
    private int countdown;
    private final boolean isHoly;
    private final long startTime;

    public FlashEffect(int x, int y, int countdown) {
        this(x, y, countdown, false);
    }

    public FlashEffect(int x, int y, int countdown, boolean isHoly) {
        this.x = x;
        this.y = y;
        this.countdown = countdown;
        this.isHoly = isHoly;
        this.startTime = System.currentTimeMillis();
    }

    public int getX() { return x; }

    public int getY() { return y; }

    public int getCountdown() { return countdown; }

    public boolean isHoly() { return isHoly; }

    public long getStartTime() { return startTime; }

    public void decrement() { countdown--; }

    public boolean isExpired() { return countdown <= 0; }
}
