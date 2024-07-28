package dk.superawesome.factorio.listeners;

import dk.superawesome.factorio.mechanics.routes.Routes;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Routes.updateNearbyRoutes(event.getBlock());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Routes.updateNearbyRoutes(event.getBlock());
    }
}
