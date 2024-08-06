package dk.superawesome.factorio.mechanics.routes;

import dk.superawesome.factorio.mechanics.SignalSource;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.mechanics.routes.events.PipePutEvent;
import dk.superawesome.factorio.util.Array;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Comparator;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.util.BlockVector;

import java.util.*;

@SuppressWarnings("unchecked")
public abstract class AbstractRoute<R extends AbstractRoute<R, P>, P extends OutputEntry> {

    private static final Map<World, Map<BlockVector, List<AbstractRoute<?, ?>>>> cachedRoutes = new HashMap<>();
    private static final Map<World, Map<BlockVector, AbstractRoute<?, ?>>> cachedOriginRoutes = new HashMap<>();

    public static <R extends AbstractRoute<R, P>, P extends OutputEntry> R getCachedOriginRoute(World world, BlockVector vec) {
        return (R) cachedOriginRoutes.computeIfAbsent(world, d -> new HashMap<>())
                .get(new BlockVector(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()));
    }

    public static List<AbstractRoute<?, ?>> getCachedRoutes(World world, BlockVector vec) {
        return cachedRoutes.computeIfAbsent(world, d -> new HashMap<>())
                .getOrDefault(new BlockVector(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()), new ArrayList<>());
    }

    public static void addRouteToCache(Block start, AbstractRoute<?, ?> route) {
        if (cachedRoutes.isEmpty()) {
            cachedRoutes.put(start.getWorld(), new HashMap<>());
        }

        cachedOriginRoutes.get(start.getWorld()).put(BlockUtil.getVec(start), route);
        for (BlockVector loc : route.getLocations()) {
            cachedRoutes.get(start.getWorld())
                    .computeIfAbsent(loc, __ -> new ArrayList<>())
                    .add(route);
        }
    }

    public static void removeRouteFromCache(World world, AbstractRoute<?, ?> route) {
        if (cachedRoutes.isEmpty()) {
            return;
        }

        cachedOriginRoutes.get(world).remove(route.getStart());
        for (BlockVector loc : route.getLocations()) {
            cachedRoutes.get(world).get(loc).remove(route);
        }
    }

    public static class TransferOutputEntry implements OutputEntry {

        private int lastRunId = -1;
        protected final World world;
        protected final BlockVector vec;

        private TransferOutputEntry(World world, BlockVector vec) {
            this.vec = vec;
            this.world = world;
        }

        public boolean handle(int runId, TransferCollection collection) {
            if (lastRunId == runId) {
                return false;
            }
            this.lastRunId = runId;

            Block block = BlockUtil.getBlock(world, vec);
            PipePutEvent event = new PipePutEvent(BlockUtil.getPointingBlock(block, false), collection);
            Bukkit.getPluginManager().callEvent(event);

            return event.transferred();
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
    protected final Array<Queue<P>> outputs = new Array<>();
    protected final Set<BlockVector> locations = new HashSet<>();

    private final BlockVector start;

    public AbstractRoute(BlockVector start) {
        this.start = start;
    }

    public BlockVector getStart() {
        return start;
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

    public Queue<P> getOutputs(int context) {
        return outputs.get(context, LinkedList::new);
    }

    public void addOutput(World world, BlockVector vec, int context) {
        outputs.get(context, LinkedList::new).add(createOutputEntry(world, vec));
    }

    public void addOutput(World world, BlockVector vec) {
        addOutput(world, vec, 0);
    }

    public void unload(Chunk chunk) {
        locations.removeIf(vec -> BlockUtil.getBlock(chunk.getWorld(), vec).getChunk().equals(chunk));
        for (Queue<P> outputs : this.outputs) {
            if (outputs != null) {
                outputs.removeIf(output -> BlockUtil.getBlock(chunk.getWorld(), output.getVec()).getChunk().equals(chunk));
            }
        }

        if (locations.isEmpty()) {
            removeRouteFromCache(chunk.getWorld(), this);
        }
    }

    public abstract RouteFactory<R> getFactory();

    public abstract void search(Block from, Material fromMat, BlockVector relVec, Block rel);

    protected abstract P createOutputEntry(World world, BlockVector vec);

    public static class Pipe extends AbstractRoute<Pipe, TransferOutputEntry> {

        public Pipe(BlockVector start) {
            super(start);
        }

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

        public boolean start(TransferCollection collection) {
            int runId = currentId++;

            boolean transferredAny = false;
            for (TransferOutputEntry entry : outputs.get(0, LinkedList::new)) {
                boolean transferred = entry.handle(runId, collection);
                if (!transferredAny && transferred) {
                    transferredAny = true;
                }

                if (collection.isTransferEmpty()) {
                    break;
                }
            }

            return transferredAny;
        }
    }

    public static class Signal extends AbstractRoute<Signal, SignalOutputEntry> {

        private final Map<BlockVector, Integer> signals = new HashMap<>();

        public Signal(BlockVector start) {
            super(start);
        }

        @Override
        public RouteFactory<Signal> getFactory() {
            return new RouteFactory.SignalRouteFactory();
        }

        @Override
        public void search(Block from, Material fromMat, BlockVector relVec, Block rel) {
            int signal = signals.getOrDefault(BlockUtil.getVec(from), 16);
            Material mat = rel.getType();
            if (mat == Material.REPEATER) {
                // check if this repeater continues the signal route or triggers an output
                Repeater repeater = (Repeater) rel.getBlockData();
                Block facing = rel.getRelative(repeater.getFacing().getOppositeFace());
                if (!rel.getRelative(repeater.getFacing()).equals(from)) {
                    // this repeater does not connect with the input
                    return;
                }

                // facing sticky piston - signal output (for power-central to mechanics)
                if (facing.getType() == Material.STICKY_PISTON) {
                    add(relVec);
                    addOutput(from.getWorld(), BlockUtil.getVec(facing), SignalSource.FROM_POWER_CENTRAL);
                    return;
                }

                // expanding signal route
                signals.put(relVec, 16); // resetting to 16 signal power
                add(relVec);
                Routes.expandRoute(this, rel);
            // comparator - signal output (for generator to power-central)
            } else if (mat == Material.COMPARATOR) {
                Comparator comparator = (Comparator) rel.getBlockData();
                Block facing = rel.getRelative(comparator.getFacing().getOppositeFace());
                // check if the comparator is facing outwards
                if (!facing.equals(from)) {
                    add(relVec);
                    addOutput(from.getWorld(), BlockUtil.getVec(facing), SignalSource.TO_POWER_CENTRAL);
                }
            } else if (mat == Material.REDSTONE_WIRE && signal > 1) {
                // expanding signal route
                add(relVec);
                signals.put(relVec, signal - 1);
                Routes.expandRoute(this, rel);
            }
        }

        @Override
        protected SignalOutputEntry createOutputEntry(World world, BlockVector vec) {
            return new SignalOutputEntry(world, vec);
        }

        public boolean start(SignalSource source) {
            int runId = currentId++;

            if (!source.preSignal(this)) {
                return false;
            }

            // handle signal outputs
            int mechanics = 0;
            for (SignalOutputEntry entry : outputs.get(source.getContext(), LinkedList::new)) {
                if (entry.handle(runId, source)) {
                    mechanics++;
                }
            }

            // power related mechanic stress
            if (outputs.get(source.getContext()).isEmpty() || mechanics < outputs.get(source.getContext()).size()) {
                source.postSignal(this, mechanics);
            }

            return mechanics > 0;
        }
    }
}
