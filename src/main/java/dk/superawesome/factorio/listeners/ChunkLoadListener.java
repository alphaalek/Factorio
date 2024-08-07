package dk.superawesome.factorio.listeners;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.MechanicManager;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkLoadListener implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        MechanicManager manager = Factorio.get().getMechanicManager(event.getWorld());
        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
            for (BlockState state : event.getChunk().getTileEntities()) {
                if (state instanceof Sign && Tag.WALL_SIGNS.isTagged(state.getType())
                        && manager.getMechanicAt(BlockUtil.getPointingBlock(state.getBlock(), false).getLocation()) == null) {
                    // load this mechanic
                    manager.loadMechanic((Sign) state);
                }
            }
        });
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        World world = event.getWorld();
        Factorio.get().getMechanicManager(world).unloadMechanics(event.getChunk());
    }
}
