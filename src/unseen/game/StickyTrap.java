package unseen.game;

/** A sticky trap placed by the Hunter that wastes one of the player's moves. */
public class StickyTrap {

    private final int x;
    private final int y;

    public StickyTrap(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}
