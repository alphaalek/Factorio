package dk.superawesome.factorio.listeners;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.MechanicManager;
import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import java.util.Collections;
import java.util.List;

public class PistonExtendListener implements Listener {

    private boolean disallowPiston(BlockPistonEvent event, List<Block> blocks) {
        MechanicManager manager = Factorio.get().getMechanicManager(event.getBlock().getWorld());
        for (Block block : blocks) {
            if (manager.getMechanicAt(block.getLocation()) != null
                    || manager.getLoadingMechanic(block.getLocation()) != null) {
                return true;
            }
        }

        for (Block block : blocks) {
            if (!AbstractRoute.getCachedRoutes(event.getBlock().getWorld(), BlockUtil.getVec(block)).isEmpty()) {
                return true;
            }
        }
        if (!AbstractRoute.getCachedRoutes(event.getBlock().getWorld(), BlockUtil.getVec(event.getBlock())).isEmpty()) {
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (disallowPiston(event, event.getBlocks())
                || disallowPiston(event, Collections.singletonList(event.getBlock()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (disallowPiston(event, event.getBlocks())
                || disallowPiston(event, Collections.singletonList(event.getBlock()))) {
            event.setCancelled(true);
        }
    }
}
