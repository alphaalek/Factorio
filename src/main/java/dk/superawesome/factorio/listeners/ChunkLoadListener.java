package dk.superawesome.factorio.listeners;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.MechanicManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.HashSet;
import java.util.Set;

public class ChunkLoadListener implements Listener {

    private static final Set<String> loadedChunks = new HashSet<>();

    private static String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getUID() + ";" + chunk.getX() + ";" + chunk.getZ();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        String key = getChunkKey(event.getChunk());
        if (loadedChunks.contains(key)) {
            // already loaded
            return;
        }
        loadedChunks.add(key);

        MechanicManager manager = Factorio.get().getMechanicManager(event.getWorld());
        Bukkit.getScheduler().runTask(Factorio.get(), () -> manager.loadMechanics(event.getChunk()));
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        // allow the chunk to be rescanned when it loads again, so any mechanic
        // that failed to load earlier can be retried
        loadedChunks.remove(getChunkKey(event.getChunk()));
    }
}
