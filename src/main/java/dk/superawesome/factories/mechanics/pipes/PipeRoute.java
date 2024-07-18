package dk.superawesome.factories.mechanics.pipes;

import dk.superawesome.factories.mechanics.ItemCollection;
import dk.superawesome.factories.mechanics.pipes.events.PipePutEvent;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;

import java.util.*;

public class PipeRoute {

    private static final Map<World, Map<BlockVector, PipeRoute>> cachedRoutes = new HashMap<>();

    public static PipeRoute getCachedRoute(World world, BlockVector vec) {
        return cachedRoutes.computeIfAbsent(world, d -> new HashMap<>())
                .get(new BlockVector(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()));
    }

    public static void addRouteToCache(World world, PipeRoute route) {
        for (BlockVector loc : route.getLocations()) {
            cachedRoutes.computeIfAbsent(world, d -> new HashMap<>())
                    .put(loc, route);
        }
        route.cached = true;
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

            Block block = BlockUtil.getBlock(world, vec);
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

    public void clear() {
        outputs.clear();
        locations.clear();
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

            if (collection.isEmpty()) {
                break;
            }
        }
    }
}
