package unseen.game;

public class Smoke {

    private int x, y;
    private int radius;
    private int duration;

    public Smoke(int x, int y, int radius, int duration) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.duration = duration;
    }

    public void decrease() {
        duration--;
    }

    public boolean isExpired() {
        return duration <= 0;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getRadius() { return radius; }
}
