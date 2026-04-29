package unseen.map;

import unseen.utils.Constants;

public class ExitPlacer {

    public static void placeExit(Map map, int floorNumber) {

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

        map.removeItem(exitX, exitY);
        map.setDecal(exitX, exitY, null);
        map.setTile(exitX, exitY, Tile.EXIT);


        // TESTING: always place a fake exit from floor 3 onward.
        if (floorNumber >= 3 && Math.random() <= 0.30) {
            placeFakeExit(map, exitX, exitY);
        }
    }

    private static void placeFakeExit(Map map, int realExitX, int realExitY) {
        int maxDistance = -1;
        int fakeX = -1;
        int fakeY = -1;

        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                if (map.getTile(x, y) != Tile.FLOOR) {
                    continue;
                }

                if (x == realExitX && y == realExitY) {
                    continue;
                }

                if (x == Constants.START_X && y == Constants.START_Y) {
                    continue;
                }

                int dist = Math.abs(x - Constants.START_X)
                        + Math.abs(y - Constants.START_Y);

                if (dist > maxDistance) {
                    maxDistance = dist;
                    fakeX = x;
                    fakeY = y;
                }
            }
        }

        if (fakeX != -1) {
            map.removeItem(fakeX, fakeY);
            map.setDecal(fakeX, fakeY, null);
            map.setTile(fakeX, fakeY, Tile.FAKE_EXIT);
        }
    }
}