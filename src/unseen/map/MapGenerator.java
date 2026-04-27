package unseen.map;

import unseen.items.Item;
import unseen.items.NoiseMaker;
import unseen.items.Shuriken;
import unseen.items.SmokeBomb;
import unseen.utils.Constants;

import java.util.Random;

public class MapGenerator {

    public static Map generate(boolean horrorMode) {

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

                // Place horror decals
                if (horrorMode) {
                    double decalRoll = rand.nextDouble();
                    if (decalRoll < 0.05) { // 5% chance per tile
                        Tile t = map.getTile(x, y);
                        if (t == Tile.WALL) {
                            map.setDecal(x, y, DecalType.BLOODY_HANDPRINT);
                        } else {
                            double subRoll = rand.nextDouble();
                            if (subRoll < 0.5) map.setDecal(x, y, DecalType.BLOOD_SPLATTER);
                            else if (subRoll < 0.7) map.setDecal(x, y, DecalType.BLOODY_TEXT_RUN);
                            else if (subRoll < 0.85) map.setDecal(x, y, DecalType.BLOODY_TEXT_HELP);
                            else if (subRoll < 0.95) map.setDecal(x, y, DecalType.BLOODY_TEXT_WATCHING);
                            else map.setDecal(x, y, DecalType.BLOODY_TEXT_HIDE);
                        }
                    }
                }
            }
        }

        // Randomly pick one of 4 corner quadrants for the start position
        // so entry and exit are always on opposite ends of the map.
        int margin = 3;
        int halfW = Constants.GRID_WIDTH / 2;
        int halfH = Constants.GRID_HEIGHT / 2;
        int quadrant = rand.nextInt(4); // 0=TL, 1=TR, 2=BL, 3=BR

        int sx = 2, sy = 2; // safe fallback
        for (int attempt = 0; attempt < 400; attempt++) {
            int qxMin = (quadrant % 2 == 0) ? margin : halfW;
            int qxMax = (quadrant % 2 == 0) ? halfW - 1 : Constants.GRID_WIDTH - margin - 1;
            int qyMin = (quadrant < 2) ? margin : halfH;
            int qyMax = (quadrant < 2) ? halfH - 1 : Constants.GRID_HEIGHT - margin - 1;
            int cx = qxMin + rand.nextInt(Math.max(1, qxMax - qxMin));
            int cy = qyMin + rand.nextInt(Math.max(1, qyMax - qyMin));
            if (map.getTile(cx, cy) == Tile.FLOOR) {
                sx = cx;
                sy = cy;
                break;
            }
        }
        Constants.START_X = sx;
        Constants.START_Y = sy;

        // Place torches
        int torchCount = 5;
        for (int i = 0; i < torchCount; i++) {
            int tx, ty;
            do {
                tx = rand.nextInt(Constants.GRID_WIDTH);
                ty = rand.nextInt(Constants.GRID_HEIGHT);
            } while (map.getTile(tx, ty) != Tile.FLOOR);
            map.setTile(tx, ty, rand.nextDouble() < 0.4 ? Tile.CAMPFIRE : Tile.TORCH);
        }

        // Place ground items (pickupable) — never on the start tile
        int itemCount = 6;
        for (int i = 0; i < itemCount; i++) {
            int tx, ty;
            do {
                tx = rand.nextInt(Constants.GRID_WIDTH);
                ty = rand.nextInt(Constants.GRID_HEIGHT);
            } while (map.getTile(tx, ty) != Tile.FLOOR
                    || (tx == Constants.START_X && ty == Constants.START_Y)
                    || map.getItem(tx, ty) != null);
            double roll = rand.nextDouble();
            Item it;
            if (roll < 0.33) {
                it = new NoiseMaker();
            } else if (roll < 0.66) {
                it = new SmokeBomb();
            } else if (roll < 0.75) {
                it = new unseen.items.Flare();
            } else {
                it = new Shuriken();
            }
            map.setItem(tx, ty, it);
        }

        // Place start tile
        map.setTile(Constants.START_X, Constants.START_Y, Tile.START);

        return map;
    }
}
