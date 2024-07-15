package dk.superawesome.factories.mehcanics.pipes;

import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.mehcanics.pipes.events.PipePutEvent;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;

import java.util.*;

public class PipeRoute {

    // TODO: cleanup cached routes when modified

    private static final Map<BlockVector, PipeRoute> cachedRoutes = new HashMap<>();

    public static PipeRoute getCachedRote(Block block) {
        return cachedRoutes.get(new BlockVector(block.getX(), block.getY(), block.getZ()));
    }

    public static void addRouteToCache(PipeRoute route) {
        for (BlockVector loc : route.getLocations()) {
            cachedRoutes.put(loc, route);
        }
        route.cached = true;
    }

    public static void removeRoutesFromCache(Chunk chunk) {
        // TODO
    }

    public static void removeRouteFromCache(Block block) {
        // TODO
    }

    private static class OutputPipeEntry {

        private int lastRunId = -1;
        protected final World world;
        protected final BlockVector vec;

        private OutputPipeEntry(World world, BlockVector vec) {
            this.vec = vec;
            this.world = world;
        }

        public void handle(int runId, ItemCollection collection) {
            if (lastRunId == runId) {
                return;
            }
            this.lastRunId = runId;

            Block block = world.getBlockAt(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());
            PipePutEvent event = new PipePutEvent(BlockUtil.getPointingBlock(block, false), collection);
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    private int currentId;
    private boolean cached;
    private final Queue<OutputPipeEntry> outputs = new LinkedList<>();
    private final Set<BlockVector> locations = new HashSet<>();

    public boolean isCached() {
        return cached;
    }

    public boolean has(BlockVector vec) {
        return locations.contains(vec);
    }

    public void add(BlockVector vec) {
        locations.add(vec);
    }

    public void addOutput(World world, BlockVector vec) {
        add(vec);
        outputs.add(new OutputPipeEntry(world, vec));
    }

    public Collection<BlockVector> getLocations() {
        return locations;
    }

    public void start(ItemCollection collection) {
        int runId = currentId++;

        for (OutputPipeEntry entry : outputs) {
            entry.handle(runId, collection);
        }
    }
}
