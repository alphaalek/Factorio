package dk.superawesome.factorio.listeners;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.Management;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.MechanicManager;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class InteractListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked != null) {
            // check if the player clicked on a mechanic
            MechanicManager manager = Factorio.get().getMechanicManager(clicked.getWorld());
            Mechanic<?, ?> mechanic = manager.getMechanicPartially(clicked.getLocation());
            if (mechanic != null) {
                event.setCancelled(true);

                // check if the player has access to open this mechanic
                if (!mechanic.getManagement().hasAccess(event.getPlayer().getUniqueId(), Management.OPEN)) {
                    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 0.5f);
                    return;
                }

                // open the mechanic inventory
                mechanic.openInventory(event.getPlayer());
            }
        }
    }
}
