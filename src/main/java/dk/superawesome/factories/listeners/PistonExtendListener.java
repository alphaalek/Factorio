package dk.superawesome.factories.listeners;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.mechanics.MechanicManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;

public class PistonExtendListener implements Listener {

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        MechanicManager manager = Factories.get().getMechanicManager(event.getBlock().getWorld());
        for (Block block : event.getBlocks()) {
            if (manager.getMechanicPartially(block.getLocation()) != null) {
                event.setCancelled(true);
            }
        }
    }
}
