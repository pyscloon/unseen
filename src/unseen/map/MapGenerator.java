package unseen.map;

import java.util.Random;
import unseen.utils.Constants;

public class MapGenerator {

    public static Map generate() {

        Map map = new Map();
        Random rand = new Random();

        // Generate walls and floors
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {

                if (x == 0 || y == 0 ||
                        x == Constants.GRID_WIDTH - 1 ||
                        y == Constants.GRID_HEIGHT - 1) {

                    map.setTile(x, y, Tile.WALL);
                } else {
                    if (rand.nextDouble() < 0.15)
                        map.setTile(x, y, Tile.WALL);
                    else
                        map.setTile(x, y, Tile.FLOOR);
                }
            }
        }

        // Place torches
        int torchCount = 5;

        for (int i = 0; i < torchCount; i++) {

            int tx, ty;

            do {
                tx = rand.nextInt(Constants.GRID_WIDTH);
                ty = rand.nextInt(Constants.GRID_HEIGHT);
            } while (map.getTile(tx, ty) != Tile.FLOOR);

            map.setTile(tx, ty, Tile.TORCH);
        }

        //  Place start tile last
        map.setTile(Constants.START_X, Constants.START_Y, Tile.START);

        return map;
    }
}
