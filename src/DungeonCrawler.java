import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Single-file prototype of the turn-based stealth dungeon crawler.
 * Compile: javac DungeonCrawler.java
 * Run:     java DungeonCrawler
 *
 * Controls (console):
 *  w = up, s = down, a = left, d = right, . = wait
 */
public class DungeonCrawler {
    /* ---------------------- Constants ---------------------- */
    static class Constants {
        static final int GRID_W = 20;
        static final int GRID_H = 20;
        static final int START_X = 2;
        static final int START_Y = 2;
        static final int MIN_ENEMIES = 3;
        static final int MAX_ENEMIES = 5;
        static final int SEARCH_TURNS = 4;
    }

    /* ---------------------- Tile ---------------------- */
    enum TileType { WALL, FLOOR, START, EXIT, ITEM }

    static class Tile {
        TileType type;
        Tile(TileType t) { this.type = t; }
        char toChar() {
            switch (type) {
                case WALL: return '#';
                case FLOOR: return '.';
                case START: return '@';
                case EXIT: return '$';
                case ITEM: return '?';
                default: return '?';
            }
        }
    }

    /* ---------------------- Map ---------------------- */
    static class GameMap {
        final int w, h;
        Tile[][] grid;
        GameMap(int w, int h) {
            this.w = w; this.h = h;
            grid = new Tile[h][w];
        }

        boolean inBounds(int x, int y) { return x >= 0 && y >= 0 && x < w && y < h; }
        boolean isPassable(int x, int y) {
            if (!inBounds(x,y)) return false;
            return grid[y][x].type != TileType.WALL;
        }

        List<int[]> allFloorTiles() {
            List<int[]> res = new ArrayList<>();
            for (int y=0;y<h;y++) for (int x=0;x<w;x++)
                if (grid[y][x].type == TileType.FLOOR || grid[y][x].type == TileType.START)
                    res.add(new int[]{x,y});
            return res;
        }

        // line of sight in same row/column until blocked by wall
        boolean hasLineOfSight(int x1, int y1, int x2, int y2) {
            if (x1 == x2) {
                int step = y2 > y1 ? 1 : -1;
                for (int y = y1 + step; y != y2 + step; y += step) {
                    if (!inBounds(x1,y)) return false;
                    if (grid[y][x1].type == TileType.WALL) return false;
                    if (y == y2) return true;
                }
            } else if (y1 == y2) {
                int step = x2 > x1 ? 1 : -1;
                for (int x = x1 + step; x != x2 + step; x += step) {
                    if (!inBounds(x,y1)) return false;
                    if (grid[y1][x].type == TileType.WALL) return false;
                    if (x == x2) return true;
                }
            }
            return false;
        }

        void prettyPrint(int px, int py, List<Enemy> enemies) {
            char[][] buf = new char[h][w];
            for (int y=0;y<h;y++) for (int x=0;x<w;x++) buf[y][x] = grid[y][x].toChar();
            // place player
            buf[py][px] = '@';
            // place enemies (G)
            for (Enemy e : enemies) buf[e.y][e.x] = 'G';
            // print coordinates header
            System.out.println();
            for (int y=0;y<h;y++) {
                for (int x=0;x<w;x++) System.out.print(buf[y][x] + " ");
                System.out.println();
            }
            System.out.println();
        }
    }

    /* ---------------------- Exit Placer ---------------------- */
    static class ExitPlacer {
        static void placeExit(GameMap map, int startX, int startY, Random rand) {
            List<int[]> floors = map.allFloorTiles();
            int maxDist = -1;
            List<int[]> candidates = new ArrayList<>();
            for (int[] f : floors) {
                int d = Math.abs(f[0] - startX) + Math.abs(f[1] - startY);
                if (d > maxDist) { maxDist = d; candidates.clear(); candidates.add(f); }
                else if (d == maxDist) candidates.add(f);
            }
            int[] chosen = candidates.get(rand.nextInt(candidates.size()));
            map.grid[chosen[1]][chosen[0]].type = TileType.EXIT;
        }
    }

    /* ---------------------- A* Node ---------------------- */
    static class Node {
        int x,y;
        double g,h,f;
        Node parent;
        Node(int x,int y){ this.x=x; this.y=y; g=h=f=0; parent=null; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Node)) return false;
            Node n=(Node)o; return n.x==x && n.y==y;
        }
        @Override public int hashCode() { return Objects.hash(x,y); }
    }

    /* ---------------------- A* Implementation ---------------------- */
    static class AStar {
        public static List<int[]> findPath(GameMap map, int sx, int sy, int tx, int ty) {
            Node start = new Node(sx,sy);
            Node target = new Node(tx,ty);
            PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
            Map<String, Node> allNodes = new HashMap<>();

            start.g = 0; start.h = heuristic(start, target); start.f = start.h;
            open.add(start); allNodes.put(key(sx,sy), start);

            Set<String> closed = new HashSet<>();

            while (!open.isEmpty()) {
                Node cur = open.poll();
                if (cur.x==tx && cur.y==ty) return reconstruct(cur);

                closed.add(key(cur.x,cur.y));

                for (int[] nb : neighbors(cur.x,cur.y, map)) {
                    int nx=nb[0], ny=nb[1];
                    String k = key(nx,ny);
                    if (closed.contains(k)) continue;
                    double tentative = cur.g + 1;
                    Node node = allNodes.getOrDefault(k, new Node(nx,ny));
                    if (!allNodes.containsKey(k) || tentative < node.g) {
                        node.g = tentative;
                        node.h = heuristic(node, target);
                        node.f = node.g + node.h;
                        node.parent = cur;
                        allNodes.put(k,node);
                        // update open (remove+add)
                        open.remove(node);
                        open.add(node);
                    }
                }
            }
            return null; // no path
        }

        private static List<int[]> reconstruct(Node cur) {
            List<int[]> p = new ArrayList<>();
            Node c = cur;
            while (c != null) { p.add(0, new int[]{c.x,c.y}); c = c.parent; }
            return p;
        }

        private static double heuristic(Node a, Node b) {
            // Manhattan (4-directional)
            return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
        }

        private static List<int[]> neighbors(int x,int y, GameMap map) {
            int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
            List<int[]> res = new ArrayList<>();
            for (int[] d : dirs) {
                int nx = x + d[0], ny = y + d[1];
                if (map.inBounds(nx,ny) && map.isPassable(nx,ny)) res.add(new int[]{nx,ny});
            }
            return res;
        }

        private static String key(int x,int y) { return x + "," + y; }
    }

    /* ---------------------- Entities ---------------------- */
    static abstract class Entity {
        int x,y;
        Entity(int x,int y){ this.x=x; this.y=y; }
    }

    /* ---------------------- Player ---------------------- */
    static class Player extends Entity {
        int invSlots = 3;
        boolean hidden = false;
        Player(int x,int y){ super(x,y); }
    }

    /* ---------------------- Enemy ---------------------- */
    static abstract class Enemy extends Entity {
        enum State { PATROL, CHASE, SEARCH }
        State state = State.PATROL;
        int detectionRange;
        List<int[]> waypoints = new ArrayList<>();
        List<int[]> cachedPath = null; // for patrol
        int pathIndex = 0;
        int searchTurnsLeft = 0;
        int lastKnownX, lastKnownY;

        Enemy(int x,int y,int detectionRange) {
            super(x,y); this.detectionRange = detectionRange;
        }

        abstract void step(GameMap map, Player player, Random rand);
        boolean seesPlayer(GameMap map, Player p) {
            if (x == p.x || y == p.y) {
                int dist = Math.abs(x - p.x) + Math.abs(y - p.y);
                if (dist <= detectionRange && map.hasLineOfSight(x,y,p.x,p.y)) return true;
            }
            return false;
        }
    }

    /* ---------------------- PatrolEnemy ---------------------- */
    static class PatrolEnemy extends Enemy {
        PatrolEnemy(int x,int y,int detectionRange, List<int[]> wps) {
            super(x,y,detectionRange);
            this.waypoints.addAll(wps);
            this.cachedPath = null;
            this.pathIndex = 0;
        }

        @Override
        void step(GameMap map, Player player, Random rand) {
            // Detection check
            if (seesPlayer(map, player)) {
                state = State.CHASE;
                lastKnownX = player.x; lastKnownY = player.y;
            }

            switch(state) {
                case PATROL:
                    // if no cached path or path exhausted, compute path to next waypoint
                    if (waypoints.isEmpty()) return;
                    int[] target = waypoints.get(pathIndex % waypoints.size());
                    if (cachedPath == null || cachedPath.isEmpty()) {
                        cachedPath = AStar.findPath(map, x, y, target[0], target[1]);
                        if (cachedPath == null) return;
                        // remove current pos from path if present
                        if (!cachedPath.isEmpty() && cachedPath.get(0)[0]==x && cachedPath.get(0)[1]==y) cachedPath.remove(0);
                    }
                    if (!cachedPath.isEmpty()) {
                        int[] step = cachedPath.remove(0);
                        x = step[0]; y = step[1];
                    } else {
                        // arrived
                        pathIndex++;
                        cachedPath = null;
                    }
                    break;
                case CHASE:
                    // Recalculate path every turn to player's current pos
                    List<int[]> path = AStar.findPath(map, x, y, player.x, player.y);
                    if (path != null && path.size() >= 2) {
                        // step one towards player
                        int[] next = path.get(1);
                        x = next[0]; y = next[1];
                    } else if (path != null && path.size()==1) {
                        // at player tile
                        x = path.get(0)[0]; y = path.get(0)[1];
                    } else {
                        // no path - go to search
                        state = State.SEARCH;
                        searchTurnsLeft = Constants.SEARCH_TURNS;
                    }
                    // update last known
                    lastKnownX = player.x; lastKnownY = player.y;
                    // if lost sight now, go to SEARCH
                    if (!seesPlayer(map, player)) { state = State.SEARCH; searchTurnsLeft = Constants.SEARCH_TURNS; }
                    break;
                case SEARCH:
                    // go to last known pos
                    if (searchTurnsLeft <= 0) { state = State.PATROL; cachedPath = null; break; }
                    List<int[]> pth = AStar.findPath(map, x, y, lastKnownX, lastKnownY);
                    if (pth != null && pth.size() >= 2) {
                        int[] next = pth.get(1);
                        x = next[0]; y = next[1];
                    }
                    searchTurnsLeft--;
                    // if spots player again, chase
                    if (seesPlayer(map, player)) { state = State.CHASE; }
                    break;
            }
        }
    }

    /* ---------------------- Map Generator (sample) ---------------------- */
    static class MapGenerator {
        static GameMap createSampleMap(Random rand) {
            int W = Constants.GRID_W, H = Constants.GRID_H;
            GameMap map = new GameMap(W,H);
            // Fill with floor
            for (int y=0;y<H;y++) for (int x=0;x<W;x++) map.grid[y][x] = new Tile(TileType.FLOOR);

            // Add outer walls
            for (int x=0;x<W;x++) { map.grid[0][x].type = TileType.WALL; map.grid[H-1][x].type = TileType.WALL; }
            for (int y=0;y<H;y++) { map.grid[y][0].type = TileType.WALL; map.grid[y][W-1].type = TileType.WALL; }

            // Add some patterned walls (like sample design) and some random walls for interest
            for (int y=2;y<H-2;y+=2) {
                for (int x=2;x<W-2;x+=3) {
                    if ((x==Constants.START_X && y==Constants.START_Y) || rand.nextDouble() < 0.15) continue;
                    map.grid[y][x].type = TileType.WALL;
                }
            }

            // add some random scatter walls
            for (int i=0;i<80;i++) {
                int rx = 1 + rand.nextInt(W-2), ry = 1 + rand.nextInt(H-2);
                if ((rx==Constants.START_X && ry==Constants.START_Y) || rx==W-2 || ry==H-2) continue;
                if (rand.nextDouble() < 0.12) map.grid[ry][rx].type = TileType.WALL;
            }

            // mark start tile
            map.grid[Constants.START_Y][Constants.START_X].type = TileType.START;
            return map;
        }

        static List<int[]> pickEnemySpawns(GameMap map, Random rand, int count) {
            List<int[]> candidates = new ArrayList<>();
            for (int y=0;y<map.h;y++) for (int x=0;x<map.w;x++) {
                if (!map.isPassable(x,y)) continue;
                if (x==Constants.START_X && y==Constants.START_Y) continue;
                int dist = Math.abs(x-Constants.START_X)+Math.abs(y-Constants.START_Y);
                if (dist < 5) continue;
                candidates.add(new int[]{x,y});
            }
            Collections.shuffle(candidates, rand);
            return candidates.subList(0, Math.min(count, candidates.size()));
        }

        static List<int[]> pickWaypoints(GameMap map, Random rand, int num) {
            List<int[]> floors = map.allFloorTiles();
            Collections.shuffle(floors, rand);
            return floors.subList(0, Math.min(num, floors.size()));
        }
    }

    /* ---------------------- Game (main loop) ---------------------- */
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        Random rand = new Random(12345); // fixed seed for reproducible maps; change or remove seed to vary

        GameMap map = MapGenerator.createSampleMap(rand);
        // place exit at furthest floor tile from start(2,2)
        ExitPlacer.placeExit(map, Constants.START_X, Constants.START_Y, rand);

        Player player = new Player(Constants.START_X, Constants.START_Y);

        // spawn enemies 3-5
        int enemyCount = Constants.MIN_ENEMIES + rand.nextInt(Constants.MAX_ENEMIES - Constants.MIN_ENEMIES + 1);
        List<int[]> spawns = MapGenerator.pickEnemySpawns(map, rand, enemyCount);
        List<Enemy> enemies = new ArrayList<>();

        for (int i=0;i<spawns.size();i++) {
            int[] s = spawns.get(i);
            // create patrol enemy with 2-4 waypoints
            int wpCount = 2 + rand.nextInt(3);
            List<int[]> wps = MapGenerator.pickWaypoints(map, rand, wpCount);
            // detection range vary slightly
            int det = 4 + rand.nextInt(4); // 4..7
            PatrolEnemy ge = new PatrolEnemy(s[0], s[1], det, wps);
            enemies.add(ge);
        }

        System.out.println("Welcome to DungeonCrawler (console prototype).");
        System.out.println("Reach the exit ($). Avoid G (enemies). Commands: w/a/s/d to move, . to wait, q to quit.");
        boolean running = true;
        int turns = 0;
        while (running) {
            map.prettyPrint(player.x, player.y, enemies);
            // status
            System.out.printf("Turn: %d  Player: (%d,%d)  Inventory slots: %d\n", turns, player.x, player.y, player.invSlots);
            System.out.print("Your move (w/a/s/d/.): ");
            String line = br.readLine();
            if (line == null) break;
            line = line.trim().toLowerCase();
            if (line.equals("q")) { System.out.println("Quitting."); break; }
            // handle first char
            char cmd = line.isEmpty() ? '.' : line.charAt(0);
            int nx = player.x, ny = player.y;
            switch (cmd) {
                case 'w': ny--; break;
                case 's': ny++; break;
                case 'a': nx--; break;
                case 'd': nx++; break;
                case '.': /* wait */ break;
                default: System.out.println("Unknown command."); continue;
            }
            // move validation
            if (map.inBounds(nx,ny) && map.isPassable(nx,ny)) {
                player.x = nx; player.y = ny;
            } else if (cmd != '.') {
                System.out.println("Can't move there (wall or out-of-bounds). Turn still consumes action.");
            }

            turns++;

            // Check win
            if (map.grid[player.y][player.x].type == TileType.EXIT) {
                System.out.println("You reached the exit! Victory!");
                System.out.printf("Turns taken: %d\n", turns);
                break;
            }

            // Enemies act
            boolean playerCaught = false;
            for (Enemy e : enemies) {
                e.step(map, player, rand);
                if (e.x == player.x && e.y == player.y) {
                    System.out.printf("Caught by enemy at (%d,%d)! Game over.\n", e.x, e.y);
                    playerCaught = true;
                    break;
                }
            }
            if (playerCaught) break;
            // after enemies move, check detection messages (optional)
            for (Enemy e : enemies) {
                if (e.seesPlayer(map, player)) {
                    System.out.println("An enemy sees you! They are chasing...");
                    // first enemy that sees the player triggers alert message
                    break;
                }
            }
        }

        System.out.println("Game ended. Thanks for playing prototype.");
    }
}
