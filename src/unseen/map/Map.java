package unseen.map;

import unseen.utils.Constants;
import unseen.items.Item;
public class Map {
    private Tile[][] grid;
    private Item[][] items;
    public Map() {
        grid = new Tile[Constants.GRID_HEIGHT][Constants.GRID_WIDTH];
        items = new Item[Constants.GRID_HEIGHT][Constants.GRID_WIDTH];
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

    public Item getItem(int x, int y) {
        if (x < 0 || y < 0 || x >= Constants.GRID_WIDTH || y >= Constants.GRID_HEIGHT) return null;
        return items[y][x];
    }

    public void setItem(int x, int y, Item item) {
        if (x < 0 || y < 0 || x >= Constants.GRID_WIDTH || y >= Constants.GRID_HEIGHT) return;
        items[y][x] = item;
    }

    public void removeItem(int x, int y) {
        if (x < 0 || y < 0 || x >= Constants.GRID_WIDTH || y >= Constants.GRID_HEIGHT) return;
        items[y][x] = null;
    }

    public Tile[][] getGrid() {
        return grid;
    }
}
