package unseen.game;

public class ActiveFlare {
    private int x;
    private int y;
    private int radius;
    private int duration;

    public ActiveFlare(int x, int y, int radius, int duration) {
        this.x = x;
        this.y = y;
        this.radius = Math.max(0, radius);
        this.duration = Math.max(1, duration);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getRadius() {
        return radius;
    }

    public int getDuration() {
        return duration;
    }

    public void decrease() {
        if (duration > 0)
            duration--;
    }

    public boolean isExpired() {
        return duration <= 0;
    }
}
