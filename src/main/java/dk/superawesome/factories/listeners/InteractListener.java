package dk.superawesome.factories.listeners;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.mehcanics.Mechanic;
import dk.superawesome.factories.mehcanics.MechanicManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class InteractListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked != null) {
            MechanicManager manager = Factories.get().getMechanicManager(clicked.getWorld());
            Mechanic<?> mechanic = manager.getMechanicPartially(clicked.getLocation());
            if (mechanic != null) {
                event.setCancelled(true);
                mechanic.openInventory(event.getPlayer());
            }
        }
    }
}
