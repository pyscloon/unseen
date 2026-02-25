package unseen.map;

import unseen.utils.Constants;

public class Map {
    private Tile[][] grid;

    public Map() {
        grid = new Tile[Constants.GRID_HEIGHT][Constants.GRID_WIDTH];
    }

    public void setTile(int x, int y, Tile tile) {
        grid[y][x] = tile;
    }

    public Tile getTile(int x, int y) {
        return grid[y][x];
    }

    public boolean isPassable(int x, int y) {
        if (x < 0 || y < 0 || x >= Constants.GRID_WIDTH || y >= Constants.GRID_HEIGHT)
            return false;
        return grid[y][x] != Tile.WALL;
    }

    public Tile[][] getGrid() {
        return grid;
    }
}
