package unseen.entities;

import java.util.List;
import unseen.ai.Pathfinder;
import unseen.map.Map;
import unseen.ai.LineOfSight;
import unseen.game.Smoke;

public abstract class Enemy extends Entity {

    public enum State { PATROL, CHASE, SEARCH }

    protected State state = State.PATROL;
    protected int detectionRange;
    protected Pathfinder pathfinder;

    protected int lastKnownX, lastKnownY;
    protected int searchTurns = 0;

    public Enemy(int x, int y, int detectionRange, Pathfinder pathfinder) {
        super(x, y);
        this.detectionRange = detectionRange;
        this.pathfinder = pathfinder;
    }

    public abstract void takeTurn(Map map, Player player, List<Smoke> smokes);

    protected boolean canSeePlayer(Map map, Player player, List<Smoke> smokes) {
        return LineOfSight.hasLineOfSight(
                map,
                x, y,
                player.getX(), player.getY(),
                detectionRange,
                smokes);
    }

    public void alertTo(int x, int y) {
        this.state = State.CHASE;
        this.lastKnownX = x;
        this.lastKnownY = y;
    }

    public void calmDown() {
        this.state = State.PATROL;
    }
}
