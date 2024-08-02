package dk.superawesome.factorio.mechanics.routes;

import dk.superawesome.factorio.mechanics.SignalSource;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.mechanics.impl.PowerCentral;
import dk.superawesome.factorio.mechanics.routes.events.PipePutEvent;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.util.BlockVector;

import java.util.*;

@SuppressWarnings("unchecked")
public abstract class AbstractRoute<R extends AbstractRoute<R, P>, P extends OutputEntry> {

    private static final Map<World, Map<BlockVector, AbstractRoute<?, ?>>> cachedRoutes = new HashMap<>();

    public static <R extends AbstractRoute<R, P>, P extends OutputEntry> R getCachedRoute(World world, BlockVector vec) {
        return (R) cachedRoutes.computeIfAbsent(world, d -> new HashMap<>())
                .get(new BlockVector(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()));
    }

    public static void addRouteToCache(Block start, AbstractRoute<?, ?> route) {
        if (cachedRoutes.isEmpty()) {
            cachedRoutes.put(start.getWorld(), new HashMap<>());
        }

        for (BlockVector loc : route.getLocations()) {
            cachedRoutes.get(start.getWorld()).put(loc, route);
        }

        route.cached = true;
    }

    public static void removeRouteFromCache(World world, AbstractRoute<?, ?> route) {
        if (cachedRoutes.isEmpty()) {
            return;
        }

        for (BlockVector loc : route.getLocations()) {
            cachedRoutes.get(world).remove(loc);
        }

        route.cached = false;
    }

    public static Collection<AbstractRoute<?, ?>> getCachedRoutes(World world) {
        return cachedRoutes.containsKey(world) ? cachedRoutes.get(world).values() : Collections.emptyList();
    }

    public static class TransferOutputEntry implements OutputEntry {

        private int lastRunId = -1;
        protected final World world;
        protected final BlockVector vec;

        private TransferOutputEntry(World world, BlockVector vec) {
            this.vec = vec;
            this.world = world;
        }

        public void handle(int runId, TransferCollection collection) {
            if (lastRunId == runId) {
                return;
            }
            this.lastRunId = runId;

            Block block = BlockUtil.getBlock(world, vec);
            PipePutEvent event = new PipePutEvent(BlockUtil.getPointingBlock(block, false), collection);
            Bukkit.getPluginManager().callEvent(event);
        }

        @Override
        public BlockVector getVec() {
            return vec;
        }
    }

    public static class SignalOutputEntry implements OutputEntry {

        private int lastRunId = -1;
        protected final World world;
        protected final BlockVector vec;

        private SignalOutputEntry(World world, BlockVector vec) {
            this.vec = vec;
            this.world = world;
        }

        public boolean handle(int runId, SignalSource source) {
            if (runId == lastRunId) {
                return false;
            }
            this.lastRunId = runId;

            Block block = BlockUtil.getBlock(world, vec);
            return source.handleOutput(block);
        }

        @Override
        public BlockVector getVec() {
            return vec;
        }
    }

    protected int currentId;
    protected boolean cached;
    protected final Queue<P> outputs = new LinkedList<>();
    protected final Set<BlockVector> locations = new HashSet<>();

    public boolean isCached() {
        return cached;
    }

    public boolean has(BlockVector vec) {
        return locations.contains(vec);
    }

    public void add(BlockVector vec) {
        locations.add(vec);
    }

    public Collection<BlockVector> getLocations() {
        return locations;
    }

    public Queue<P> getOutputs() {
        return outputs;
    }

    public void addOutput(World world, BlockVector vec) {
        outputs.add(createOutputEntry(world, vec));
    }

    public void unload(Chunk chunk) {
        locations.removeIf(vec -> BlockUtil.getBlock(chunk.getWorld(), vec).getChunk().equals(chunk));
        outputs.removeIf(output -> BlockUtil.getBlock(chunk.getWorld(), output.getVec()).getChunk().equals(chunk));

        if (locations.isEmpty()) {
            removeRouteFromCache(chunk.getWorld(), this);
        }
    }

    public abstract RouteFactory<R> getFactory();

    public abstract void search(Block from, Material fromMat, BlockVector relVec, Block rel);

    protected abstract P createOutputEntry(World world, BlockVector vec);


    public static class Pipe extends AbstractRoute<Pipe, TransferOutputEntry> {

        @Override
        public RouteFactory<Pipe> getFactory() {
            return new RouteFactory.PipeRouteFactory();
        }

        @Override
        public void search(Block from, Material fromMat, BlockVector relVec, Block rel) {
            Material mat = rel.getType();

            // piston = pipe output
            if (mat == Material.PISTON) {
                add(relVec);
                addOutput(from.getWorld(), relVec);
            // glass = pipe expand
            } else if (
                    mat == Material.GLASS
                    || BlockUtil.anyStainedGlass.test(mat)
                        && (fromMat == mat
                            || fromMat == Material.GLASS
                            || fromMat == Material.STICKY_PISTON
                        )
            ) {
                add(relVec);
                Routes.expandRoute(this, rel);
            }
        }

        @Override
        protected TransferOutputEntry createOutputEntry(World world, BlockVector vec) {
            return new TransferOutputEntry(world, vec);
        }

        public void start(TransferCollection collection) {
            int runId = currentId++;

            for (TransferOutputEntry entry : outputs) {
                entry.handle(runId, collection);

                if (collection.isTransferEmpty()) {
                    break;
                }
            }
        }
    }

    public static class Signal extends AbstractRoute<Signal, SignalOutputEntry> {


        @Override
        public RouteFactory<Signal> getFactory() {
            return new RouteFactory.SignalRouteFactory();
        }

        @Override
        public void search(Block from, Material fromMat, BlockVector relVec, Block rel) {
            Material mat = rel.getType();

            boolean expand = false;
            if (mat == Material.REPEATER) {
                // check if this repeater continues the signal route or triggers an output
                Repeater repeater = (Repeater) rel.getBlockData();
                Block facing = rel.getRelative(repeater.getFacing().getOppositeFace());
                switch (facing.getType()) {
                    // redstone or repeater - signal expand
                    case REDSTONE_WIRE:
                    case REPEATER:
                        expand = true;
                        break;
                    // facing sticky piston - signal output
                    case STICKY_PISTON:
                        add(relVec);
                        addOutput(from.getWorld(), BlockUtil.getVec(facing));
                        return;
                }
            }

            if (mat == Material.REDSTONE_WIRE || expand) {
                add(relVec);
                Routes.expandRoute(this, rel);
            }
        }

        @Override
        protected SignalOutputEntry createOutputEntry(World world, BlockVector vec) {
            return new SignalOutputEntry(world, vec);
        }

        public void start(SignalSource source) {
            int runId = currentId++;

            if (!source.preSignal(this)) {
                return;
            }

            // handle signal outputs
            int mechanics = 0;
            for (SignalOutputEntry entry : outputs) {
                if (entry.handle(runId, source)) {
                    mechanics++;
                }
            }

            // power related mechanic stress
            if (mechanics < outputs.size() && outputs.size() > 1) {
                source.postSignal(this, mechanics);
            }
        }
    }
}
