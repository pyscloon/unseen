package unseen.map;

import unseen.items.Item;
import unseen.items.GrapplingHook;
import unseen.items.NoiseMaker;
import unseen.items.Shuriken;
import unseen.items.SmokeBomb;
import unseen.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapGenerator {

    private static final int MIN_ROOM_SIZE = 4;
    private static final int MAX_ROOM_SIZE = 9;
    private static final int ROOM_ATTEMPTS = 60;
    private static final int TARGET_ROOMS = 11;
    private static final int HALLWAY_WIDTH = 2;
    private static final int ROOM_SPACING = 2;

    private static class Room {
        final int x;
        final int y;
        final int width;
        final int height;

        Room(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        int centerX() {
            return x + width / 2;
        }

        int centerY() {
            return y + height / 2;
        }

        int innerLeft() {
            return x + 1;
        }

        int innerRight() {
            return x + width - 2;
        }

        int innerTop() {
            return y + 1;
        }

        int innerBottom() {
            return y + height - 2;
        }

        boolean intersects(Room other) {
            return x - ROOM_SPACING <= other.x + other.width - 1
                    && x + width - 1 + ROOM_SPACING >= other.x
                    && y - ROOM_SPACING <= other.y + other.height - 1
                    && y + height - 1 + ROOM_SPACING >= other.y;
        }
    }

    public static Map generate(boolean horrorMode) {

        Map map = new Map();
        Random rand = new Random();
        List<Room> rooms = new ArrayList<>();

        // Start with solid walls, then carve out rooms and hallways.
        for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
            for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                map.setTile(x, y, Tile.WALL);
            }
        }

        placeRoomInRegion(map, rooms, rand, 0, 0, Constants.GRID_WIDTH / 2, Constants.GRID_HEIGHT / 2);
        placeRoomInRegion(map, rooms, rand, Constants.GRID_WIDTH / 2, 0, Constants.GRID_WIDTH, Constants.GRID_HEIGHT / 2);
        placeRoomInRegion(map, rooms, rand, 0, Constants.GRID_HEIGHT / 2, Constants.GRID_WIDTH / 2, Constants.GRID_HEIGHT);
        placeRoomInRegion(map, rooms, rand, Constants.GRID_WIDTH / 2, Constants.GRID_HEIGHT / 2, Constants.GRID_WIDTH, Constants.GRID_HEIGHT);

        for (int attempt = 0; attempt < ROOM_ATTEMPTS && rooms.size() < TARGET_ROOMS; attempt++) {
            tryPlaceRoom(map, rooms, rand, 1, 1, Constants.GRID_WIDTH - 1, Constants.GRID_HEIGHT - 1);
        }

        if (rooms.isEmpty()) {
            Room fallback = new Room(2, 2, Constants.GRID_WIDTH - 4, Constants.GRID_HEIGHT - 4);
            carveRoomInterior(map, fallback);
            rooms.add(fallback);
        }

        connectRooms(map, rooms, rand);

        Room startRoom = chooseStartRoom(rooms, rand);
        Constants.START_X = startRoom.centerX();
        Constants.START_Y = startRoom.centerY();

        // Place horror decals after the layout exists so walls and floors get fitting marks.
        if (horrorMode) {
            for (int y = 0; y < Constants.GRID_HEIGHT; y++) {
                for (int x = 0; x < Constants.GRID_WIDTH; x++) {
                    double decalRoll = rand.nextDouble();
                    if (decalRoll < 0.08) {
                        Tile t = map.getTile(x, y);
                        if (t == Tile.WALL) {
                            map.setDecal(x, y, DecalType.BLOODY_HANDPRINT);
                        } else {
                            double subRoll = rand.nextDouble();
                            if (subRoll < 0.4) map.setDecal(x, y, DecalType.BLOOD_SPLATTER);
                            else if (subRoll < 0.7) map.setDecal(x, y, DecalType.BLOOD_TILE);
                            else if (subRoll < 0.85) map.setDecal(x, y, DecalType.DIE_TILE);
                            else if (subRoll < 0.90) map.setDecal(x, y, DecalType.BLOODY_TEXT_RUN);
                            else if (subRoll < 0.94) map.setDecal(x, y, DecalType.BLOODY_TEXT_HELP);
                            else if (subRoll < 0.97) map.setDecal(x, y, DecalType.BLOODY_TEXT_WATCHING);
                            else map.setDecal(x, y, DecalType.BLOODY_TEXT_HIDE);
                        }
                    }
                }
            }
        }

        // Place torches/campfires
        int torchCount = horrorMode ? 3 : 5;
        for (int i = 0; i < torchCount; i++) {
            int tx, ty;
            do {
                tx = rand.nextInt(Constants.GRID_WIDTH);
                ty = rand.nextInt(Constants.GRID_HEIGHT);
            } while (map.getTile(tx, ty) != Tile.FLOOR);
            
            if (horrorMode) {
                map.setTile(tx, ty, Tile.CAMPFIRE);
            } else {
                map.setTile(tx, ty, rand.nextDouble() < 0.4 ? Tile.CAMPFIRE : Tile.TORCH);
            }
        }

        // Place ground items (pickupable) -- never on the start tile
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
            if (horrorMode && rand.nextDouble() < 0.03) {
                it = new unseen.items.Cross();
            } else if (roll < 0.33) {
                it = new NoiseMaker();
            } else if (roll < 0.66) {
                it = new SmokeBomb();
            } else if (roll < 0.75) {
                it = new unseen.items.Flare();
            } else if (roll < 0.88) {
                it = new GrapplingHook();
            } else {
                it = new Shuriken();
            }
            map.setItem(tx, ty, it);
        }

        // Place a few "Notes" for environmental storytelling
        int noteCount = horrorMode ? 2 : 1;
        for (int i = 0; i < noteCount; i++) {
            int nx, ny;
            int attempts = 0;
            do {
                nx = rand.nextInt(Constants.GRID_WIDTH);
                ny = rand.nextInt(Constants.GRID_HEIGHT);
                attempts++;
            } while (attempts < 100 && (map.getTile(nx, ny) != Tile.FLOOR || map.getDecal(nx, ny) != null));
            
            if (attempts < 100) {
                map.setDecal(nx, ny, DecalType.NOTE_SCRAP);
            }
        }

        // Place start tile
        map.setTile(Constants.START_X, Constants.START_Y, Tile.START);

        return map;
    }

    private static void carveRoomInterior(Map map, Room room) {
        for (int y = room.innerTop(); y <= room.innerBottom(); y++) {
            for (int x = room.innerLeft(); x <= room.innerRight(); x++) {
                map.setTile(x, y, Tile.FLOOR);
            }
        }
    }

    private static void placeRoomInRegion(Map map, List<Room> rooms, Random rand,
                                          int minX, int minY, int maxX, int maxY) {
        for (int attempt = 0; attempt < ROOM_ATTEMPTS && rooms.size() < TARGET_ROOMS; attempt++) {
            if (tryPlaceRoom(map, rooms, rand, minX, minY, maxX, maxY)) {
                return;
            }
        }
    }

    private static boolean tryPlaceRoom(Map map, List<Room> rooms, Random rand,
                                        int minX, int minY, int maxX, int maxY) {
        int regionWidth = maxX - minX;
        int regionHeight = maxY - minY;
        if (regionWidth < MIN_ROOM_SIZE + 2 || regionHeight < MIN_ROOM_SIZE + 2) {
            return false;
        }

        int maxRoomWidth = Math.min(MAX_ROOM_SIZE, regionWidth - 2);
        int maxRoomHeight = Math.min(MAX_ROOM_SIZE, regionHeight - 2);
        if (maxRoomWidth < MIN_ROOM_SIZE || maxRoomHeight < MIN_ROOM_SIZE) {
            return false;
        }

        int width = MIN_ROOM_SIZE + rand.nextInt(maxRoomWidth - MIN_ROOM_SIZE + 1);
        int height = MIN_ROOM_SIZE + rand.nextInt(maxRoomHeight - MIN_ROOM_SIZE + 1);
        int startX = Math.max(1, minX + 1);
        int startY = Math.max(1, minY + 1);
        int endX = Math.min(Constants.GRID_WIDTH - width - 1, maxX - width - 1);
        int endY = Math.min(Constants.GRID_HEIGHT - height - 1, maxY - height - 1);
        if (endX < startX || endY < startY) {
            return false;
        }

        int x = startX + rand.nextInt(endX - startX + 1);
        int y = startY + rand.nextInt(endY - startY + 1);
        Room room = new Room(x, y, width, height);
        boolean overlaps = rooms.stream().anyMatch(existing -> existing.intersects(room));
        if (overlaps) {
            return false;
        }

        carveRoomInterior(map, room);
        rooms.add(room);
        return true;
    }

    private static void carveHallwayBetweenRooms(Map map, Room from, Room to, Random rand) {
        int x1;
        int y1;
        int x2;
        int y2;

        if (Math.abs(from.centerX() - to.centerX()) >= Math.abs(from.centerY() - to.centerY())) {
            boolean toRight = to.centerX() > from.centerX();
            x1 = toRight ? from.innerRight() + 1 : from.innerLeft() - 1;
            y1 = clamp(to.centerY(), from.innerTop(), from.innerBottom());
            x2 = toRight ? to.innerLeft() - 1 : to.innerRight() + 1;
            y2 = clamp(from.centerY(), to.innerTop(), to.innerBottom());
        } else {
            boolean toBottom = to.centerY() > from.centerY();
            x1 = clamp(to.centerX(), from.innerLeft(), from.innerRight());
            y1 = toBottom ? from.innerBottom() + 1 : from.innerTop() - 1;
            x2 = clamp(from.centerX(), to.innerLeft(), to.innerRight());
            y2 = toBottom ? to.innerTop() - 1 : to.innerBottom() + 1;
        }

        if (rand.nextBoolean()) {
            carveHorizontal(map, x1, x2, y1);
            carveVertical(map, y1, y2, x2);
        } else {
            carveVertical(map, y1, y2, x1);
            carveHorizontal(map, x1, x2, y2);
        }
    }

    private static void carveHorizontal(Map map, int x1, int x2, int y) {
        int from = Math.min(x1, x2);
        int to = Math.max(x1, x2);
        for (int x = from; x <= to; x++) {
            for (int offset = 0; offset < HALLWAY_WIDTH; offset++) {
                int hallY = y + offset;
                if (hallY > 0 && hallY < Constants.GRID_HEIGHT - 1) {
                    map.setTile(x, hallY, Tile.FLOOR);
                }
            }
        }
    }

    private static void carveVertical(Map map, int y1, int y2, int x) {
        int from = Math.min(y1, y2);
        int to = Math.max(y1, y2);
        for (int y = from; y <= to; y++) {
            for (int offset = 0; offset < HALLWAY_WIDTH; offset++) {
                int hallX = x + offset;
                if (hallX > 0 && hallX < Constants.GRID_WIDTH - 1) {
                    map.setTile(hallX, y, Tile.FLOOR);
                }
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void connectRooms(Map map, List<Room> rooms, Random rand) {
        if (rooms.size() < 2) {
            return;
        }

        List<Room> sorted = new ArrayList<>(rooms);
        sorted.sort((a, b) -> Integer.compare(a.centerX(), b.centerX()));

        for (int i = 1; i < sorted.size(); i++) {
            carveHallwayBetweenRooms(map, sorted.get(i - 1), sorted.get(i), rand);
        }

        Room topMost = sorted.get(0);
        Room bottomMost = sorted.get(0);
        for (Room room : sorted) {
            if (room.centerY() < topMost.centerY()) {
                topMost = room;
            }
            if (room.centerY() > bottomMost.centerY()) {
                bottomMost = room;
            }
        }

        if (topMost != bottomMost) {
            carveHallwayBetweenRooms(map, topMost, bottomMost, rand);
        }
    }

    private static Room chooseStartRoom(List<Room> rooms, Random rand) {
        int halfW = Constants.GRID_WIDTH / 2;
        int halfH = Constants.GRID_HEIGHT / 2;
        int quadrant = rand.nextInt(4);

        List<Room> matches = new ArrayList<>();
        for (Room room : rooms) {
            int cx = room.centerX();
            int cy = room.centerY();
            boolean left = cx < halfW;
            boolean top = cy < halfH;

            if ((quadrant == 0 && left && top)
                    || (quadrant == 1 && !left && top)
                    || (quadrant == 2 && left && !top)
                    || (quadrant == 3 && !left && !top)) {
                matches.add(room);
            }
        }

        if (!matches.isEmpty()) {
            return matches.get(rand.nextInt(matches.size()));
        }
        return rooms.get(rand.nextInt(rooms.size()));
    }
}
