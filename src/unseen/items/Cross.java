package unseen.items;

import unseen.entities.*;
import unseen.map.Map;
import java.util.List;

/**
 * The Holy Cross is a rare powerful item that purifies the current floor.
 * When used, it:
 * 1. Removes all blood and horror decals.
 * 2. Banishes supernatural entities (Stalkers).
 * 3. Restores light and stabilizes the atmosphere via LevelManager.
 */
public class Cross extends Item {
    
    @Override
    public void use(Player player, Map map, List<Enemy> enemies) {
        unseen.utils.SoundManager.get().play("holy_cross", 1.0f); 
        
        // 1. Clear the map decals
        map.clearAllDecals();
        
        // 2. Remove horror-only enemies (Stalker)
        enemies.removeIf(e -> e.getType() == Enemy.EnemyType.STALKER);
        
        // 3. Trigger purification in LevelManager via SmokeSpawner interface
        if (player.getSmokeSpawner() != null) {
            player.getSmokeSpawner().purifyFloor();
            
            // 4. Visual feedback
            player.getSmokeSpawner().addHolyFlash(player.getX(), player.getY());
        }
    }
}
