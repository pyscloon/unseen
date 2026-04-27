package unseen.items;

public class Heart extends Item {
    @Override
    public void use(unseen.entities.Player player, unseen.map.Map map, java.util.List<unseen.entities.Enemy> enemies) {
        // Heart is consumed immediately on pickup in GamePanel.attemptPickup()
    }
}
