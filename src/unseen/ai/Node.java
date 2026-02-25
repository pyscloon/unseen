package unseen.ai;

public class Node {
    public int x, y;
    public double gCost, hCost, fCost;
    public Node parent;

    public Node(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
