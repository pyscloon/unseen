package unseen.game;

public class Smoke {

    private int x, y;
    private int radius;
    private int duration;
    private boolean skipNextDecrease;

    public Smoke(int x, int y, int radius, int duration) {
        this(x, y, radius, duration, false);
    }

    public Smoke(int x, int y, int radius, int duration, boolean skipNextDecrease) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.duration = duration;
        this.skipNextDecrease = skipNextDecrease;
    }

    public void decrease() {
        if (skipNextDecrease) {
            skipNextDecrease = false;
            return;
        }
        duration--;
    }

    public boolean isExpired() {
        return duration <= 0;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getRadius() { return radius; }
}
