package dk.superawesome.factories.production.pipes;

import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.production.pipes.events.PipePutEvent;
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

    private static class PipeEntry {

        private int lastRunId;
        protected final World world;
        protected final BlockVector vec;

        private PipeEntry(World world, BlockVector vector) {
            this.vec = vector;
            this.world = world;
        }

        public boolean handle(int runId, ItemCollection collection) {
            if (lastRunId == runId) {
                return false;
            }

            lastRunId = runId;
            return true;
        }
    }

    private static class OutputPipeEntry extends PipeEntry {

        private OutputPipeEntry(World world, BlockVector vector) {
            super(world, vector);
        }

        @Override
        public boolean handle(int runId, ItemCollection collection) {
            if (!super.handle(runId, collection)) {
                return false;
            }

            PipePutEvent event = new PipePutEvent(world.getBlockAt(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()), collection);
            Bukkit.getPluginManager().callEvent(event);

            return true;
        }
    }

    private int currentId;
    private boolean cached;
    private final Map<BlockVector, Queue<PipeEntry>> locations = new HashMap<>();

    public boolean isCached() {
        return cached;
    }

    public boolean hasLocation(BlockVector vector) {
        return locations.containsKey(vector);
    }

    private void add(BlockVector from, PipeEntry entry) {
        locations.computeIfAbsent(from, d -> new LinkedList<>())
                .add(entry);
    }

    public void addLocation(World world, BlockVector from, BlockVector vector) {
        add(from, new PipeEntry(world, vector));
    }

    public void addOutputLocation(World world, BlockVector from, BlockVector vector) {
        add(from, new OutputPipeEntry(world, vector));
    }

    public Collection<BlockVector> getLocations() {
        return locations.keySet();
    }

    public void start(Block block, ItemCollection collection) {
        run(currentId++, BlockUtil.getVec(block), collection);
    }

    public void run(int runId, BlockVector vec, ItemCollection collection) {
        Queue<PipeEntry> entries = locations.get(vec);
        // TODO: optimize pipe route
        for (PipeEntry entry : entries) {
            if (entry.handle(runId, collection)) {
                run(runId, entry.vec, collection);
            }
        }
    }
}
