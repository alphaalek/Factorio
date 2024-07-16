package dk.superawesome.factories.listeners;

import dk.superawesome.factories.mehcanics.pipes.Pipes;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Pipes.updateNearbyRoutes(event.getBlock());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Pipes.updateNearbyRoutes(event.getBlock());
    }
}
