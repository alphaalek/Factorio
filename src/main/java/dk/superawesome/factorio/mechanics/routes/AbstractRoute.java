package dk.superawesome.factorio.mechanics.routes;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.SignalInvoker;
import dk.superawesome.factorio.mechanics.SignalSource;
import dk.superawesome.factorio.mechanics.Source;
import dk.superawesome.factorio.mechanics.impl.power.PowerCentral;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.Array;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.util.BlockVector;

import java.util.*;

@SuppressWarnings("unchecked")
public abstract class AbstractRoute<R extends AbstractRoute<R, O>, O> {

    private static final Map<World, Map<BlockVector, List<AbstractRoute<?, ?>>>> cachedRoutes = new HashMap<>();
    private static final Map<World, Map<BlockVector, AbstractRoute<?, ?>>> cachedOriginRoutes = new HashMap<>();

    public static <R extends AbstractRoute<R, ?>> R getCachedOriginRoute(World world, BlockVector vec) {
        return (R) cachedOriginRoutes.computeIfAbsent(world, d -> new HashMap<>())
                .get(new BlockVector(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()));
    }

    public static List<AbstractRoute<?, ?>> getCachedRoutes(World world, BlockVector vec) {
        return cachedRoutes.computeIfAbsent(world, d -> new HashMap<>())
                .getOrDefault(new BlockVector(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()), new ArrayList<>());
    }

    public static <R extends AbstractRoute<R, ?>> void addRouteToCache(AbstractRoute<?, ?> route) {
        if (cachedRoutes.isEmpty()) {
            cachedRoutes.put(route.getWorld(), new HashMap<>());
        }

        if (!route.getLocations().isEmpty()) {
            cachedOriginRoutes.get(route.getWorld()).put(route.getStart(), route);
            for (BlockVector loc : route.getLocations()) {
                cachedRoutes.get(route.getWorld())
                        .computeIfAbsent(loc, __ -> new ArrayList<>())
                        .add(route);
            }

            if (route.getFactory().getEventHandler() != null) {
                ((R) route).getFactory().getEventHandler().callBuildEvent((R) route);
            }
        }
    }

    public static <R extends AbstractRoute<R, ?>> void removeRouteFromCache(AbstractRoute<?, ?> route) {
        if (cachedRoutes.isEmpty()) {
            return;
        }

        cachedOriginRoutes.get(route.getWorld()).remove(route.getStart());
        for (BlockVector loc : route.getLocations()) {
            cachedRoutes.get(route.getWorld()).getOrDefault(loc, new ArrayList<>()).remove(route);
        }

        if (route.getFactory().getEventHandler() != null) {
            ((R) route).getFactory().getEventHandler().callRemoveEvent((R) route);
        }
    }

    protected final Array<Queue<O>> outputs = new Array<>();
    protected final Set<BlockVector> locations = new HashSet<>();
    protected final Map<BlockVector, List<BlockVector>> visited = new HashMap<>();

    private final World world;
    private final BlockVector start;

    public AbstractRoute(World world, BlockVector start) {
        this.start = start;
        this.world = world;
    }

    public World getWorld() {
        return world;
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

    public Queue<O> getOutputs(int context) {
        return outputs.get(context, LinkedList::new);
    }

    public void addOutput(World world, BlockVector vec, BlockVector from, int context) {
        outputs.get(context, LinkedList::new).add(createOutputEntry(world, vec, from));
        locations.add(vec);
    }

    public void addOutput(World world, BlockVector vec, BlockVector from) {
        addOutput(world, vec, from, Routes.DEFAULT_CONTEXT);
    }

    public abstract RouteFactory<R> getFactory();

    public abstract void search(Block from, BlockVector relVec, Block rel, boolean isFromOrigin);

    protected abstract O createOutputEntry(World world, BlockVector vec, BlockVector from);
}
