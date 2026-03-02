package unseen.ai;

import unseen.map.Map;
import unseen.map.Tile;
import unseen.utils.Constants;

import java.util.List;

public class PathValidator {

    private final AStar pathfinder = new AStar();

    public boolean isValid(Map map) {

        int startX = Constants.START_X;
        int startY = Constants.START_Y;

        int exitX = -1;
        int exitY = -1;

        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {

                if (map.getTile(x, y) == Tile.EXIT) {
                    exitX = x;
                    exitY = y;
                    break;
                }
            }
        }

        if (exitX == -1) return false;

        List<Node> path =
                pathfinder.findPath(map, startX, startY, exitX, exitY);

        return path != null;
    }
}