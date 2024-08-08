package dk.superawesome.factorio.mechanics.routes;

import dk.superawesome.factorio.mechanics.SignalSource;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.util.Array;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BlockVector;

import java.util.*;

@SuppressWarnings("unchecked")
public abstract class AbstractRoute<R extends AbstractRoute<R, P>, P extends OutputEntry> {

    private static final Map<World, Map<BlockVector, List<AbstractRoute<?, ?>>>> cachedRoutes = new HashMap<>();
    private static final Map<World, Map<BlockVector, AbstractRoute<?, ?>>> cachedOriginRoutes = new HashMap<>();

    public static <R extends AbstractRoute<R, ? extends OutputEntry>> R getCachedOriginRoute(World world, BlockVector vec) {
        return (R) cachedOriginRoutes.computeIfAbsent(world, d -> new HashMap<>())
                .get(new BlockVector(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()));
    }

    public static List<AbstractRoute<?, ?>> getCachedRoutes(World world, BlockVector vec) {
        return cachedRoutes.computeIfAbsent(world, d -> new HashMap<>())
                .getOrDefault(new BlockVector(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()), new ArrayList<>());
    }

    public static <R extends AbstractRoute<R, ? extends OutputEntry>> void addRouteToCache(World world, AbstractRoute<?, ?> route) {
        if (cachedRoutes.isEmpty()) {
            cachedRoutes.put(world, new HashMap<>());
        }

        if (!route.getLocations().isEmpty()) {
            cachedOriginRoutes.get(world).put(route.getStart(), route);
            for (BlockVector loc : route.getLocations()) {
                cachedRoutes.get(world)
                        .computeIfAbsent(loc, __ -> new ArrayList<>())
                        .add(route);
            }

            ((R) route).getFactory().callBuildEvent((R) route);
        }
    }

    public static <R extends AbstractRoute<R, ? extends OutputEntry>> void removeRouteFromCache(World world, AbstractRoute<?, ?> route) {
        if (cachedRoutes.isEmpty()) {
            return;
        }

        cachedOriginRoutes.get(world).remove(route.getStart());
        for (BlockVector loc : route.getLocations()) {
            cachedRoutes.get(world).getOrDefault(loc, new ArrayList<>()).remove(route);
        }

        ((R) route).getFactory().callRemoveEvent((R) route);
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
    protected final Map<BlockVector, List<BlockVector>> visited = new HashMap<>();

    private final BlockVector start;

    public AbstractRoute(BlockVector start) {
        this.start = start;
    }

    public BlockVector getStart() {
        return start;
    }

    public boolean hasVisited(BlockVector vec, BlockVector rel) {
        return visited.containsKey(vec) && visited.get(vec).contains(rel);
    }

    public void visit(BlockVector vec, BlockVector rel) {
        visited.computeIfAbsent(vec, __ -> new ArrayList<>()).add(rel);
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

            boolean transferred = false;
            for (TransferOutputEntry entry : outputs.get(0, LinkedList::new)) {
                if (entry.handle(runId, collection)) {
                    transferred = true;
                }

                if (collection.isTransferEmpty()) {
                    break;
                }
            }

            return transferred;
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
                if (!BlockUtil.getPointingBlock(rel, false).equals(from)) {
                    // this repeater does not connect with the input
                    return;
                }

                Block facing = BlockUtil.getPointingBlock(rel, true);
                // facing sticky piston - signal output (for power-central to mechanics)
                if (facing.getType() == Material.STICKY_PISTON) {
                    add(relVec);
                    addOutput(from.getWorld(), BlockUtil.getVec(facing), SignalSource.FROM_POWER_CENTRAL);
                    return;
                }

                addWire(rel, relVec, 16);

            // comparator - signal output (for generator to power-central)
            } else if (mat == Material.COMPARATOR) {
                Block facing = BlockUtil.getPointingBlock(rel, true);
                // check if the comparator is facing outwards
                if (!facing.equals(from)) {
                    add(relVec);
                    addOutput(from.getWorld(), BlockUtil.getVec(facing), SignalSource.TO_POWER_CENTRAL);
                }

            // check for expand signal route
            } else if (signal > 1) {
                if (mat == Material.REDSTONE_WIRE) {
                    addWire(rel, relVec, signal - 1);
                } else {
                    Block up = rel.getRelative(BlockFace.UP);
                    Block down = rel.getRelative(BlockFace.DOWN);

                    if (up.getType() == Material.REDSTONE_WIRE) {
                        addWire(up, BlockUtil.getVec(up), signal - 1);
                    }
                    if (from.getType() == Material.REDSTONE_WIRE && (down.getType() == Material.REDSTONE_WIRE || down.getType() == Material.REPEATER)) {
                        addWire(down, BlockUtil.getVec(down), signal - 1);
                    }
                }
            }
        }

        private void addWire(Block rel, BlockVector relVec, int signal) {
            add(relVec);
            signals.put(relVec, signal);
            Routes.expandRoute(this, rel);
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
