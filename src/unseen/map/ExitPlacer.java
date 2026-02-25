package unseen.map;

import unseen.utils.Constants;

public class ExitPlacer {

    public static void placeExit(Map map) {

        int maxDistance = -1;
        int exitX = 0;
        int exitY = 0;

        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {

                if (map.getTile(x, y) == Tile.FLOOR) {

                    int dist = Math.abs(x - Constants.START_X)
                            + Math.abs(y - Constants.START_Y);

                    if (dist > maxDistance) {
                        maxDistance = dist;
                        exitX = x;
                        exitY = y;
                    }
                }
            }
        }

        map.setTile(exitX, exitY, Tile.EXIT);
    }
}
