package dk.superawesome.factorio.listeners;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.MechanicManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkLoadListener implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        MechanicManager manager = Factorio.get().getMechanicManager(event.getWorld());
        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
            manager.loadMechanics(event.getChunk());
        });
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        World world = event.getWorld();
        Factorio.get().getMechanicManager(world).unloadMechanics(event.getChunk());
    }
}
