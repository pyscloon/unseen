package unseen.utils;

public class Constants {
    public static final int GRID_WIDTH = 25;
    public static final int GRID_HEIGHT = 25;
    public static final int TILE_SIZE = 32;

    public static int START_X = 2;
    public static int START_Y = 2;

    public static final int MIN_ENEMIES = 5;
    public static final int MAX_ENEMIES = 10;

    public static final int PATROL_DETECTION_RANGE  = 5;
    public static final int HUNTER_DETECTION_RANGE  = 7;
    public static final int SENTRY_DETECTION_RANGE  = 4;
    public static final int CRAWLER_PROXIMITY_RANGE = 3; // Manhattan tiles — no LOS

    public static final int SEARCH_TURNS = 4;

    public static final int WINDOW_WIDTH  = GRID_WIDTH  * TILE_SIZE;
    public static final int WINDOW_HEIGHT = GRID_HEIGHT * TILE_SIZE;
}