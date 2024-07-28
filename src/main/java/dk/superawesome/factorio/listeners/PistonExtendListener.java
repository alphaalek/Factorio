package dk.superawesome.factorio.listeners;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.MechanicManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;

public class PistonExtendListener implements Listener {

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        MechanicManager manager = Factorio.get().getMechanicManager(event.getBlock().getWorld());
        for (Block block : event.getBlocks()) {
            if (manager.getMechanicPartially(block.getLocation()) != null) {
                event.setCancelled(true);
            }
        }
    }
}
