package dk.superawesome.factories.mechanics.routes;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.mechanics.ItemCollection;
import dk.superawesome.factories.mechanics.impl.PowerCentral;
import dk.superawesome.factories.mechanics.routes.events.PipeSuckEvent;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

public class Routes {

    private static final RouteFactory<AbstractRoute.Pipe, AbstractRoute.ItemsOutputEntry> itemsRouteFactory = new RouteFactory.PipeRouteFactory();
    private static final RouteFactory<AbstractRoute.Signal, AbstractRoute.SignalOutputEntry> signalRouteFactory = new RouteFactory.SignalRouteFactory();

    public static void suckItems(Block start, PowerCentral source) {
        if (start.getType() != Material.STICKY_PISTON) {
            return;
        }

        // we will suck items out of the mechanic that the sticky piston is pointing towards
        Block from = BlockUtil.getPointingBlock(start, false);
        if (from == null) {
            return;
        }

        PipeSuckEvent event = new PipeSuckEvent(from);
        Bukkit.getPluginManager().callEvent(event);
        if (event.getItems() == null
                || event.getItems().isEmpty()
                || source.getEnergy() < event.getItems().getEnergyCost()) {
            return;
        }

        source.setEnergy(source.getEnergy() - event.getItems().getEnergyCost());

        // start the pipe route
        startItemsRoute(start, event.getItems());
    }

    private static <R extends AbstractRoute<R, P>, P extends OutputEntry> R setupRoute(Block start, RouteFactory<R, P> factory) {
        R route = AbstractRoute.getCachedRoute(start.getWorld(), BlockUtil.getVec(start));
        if (route == null) {
            route = createNewRoute(start, factory);
        }

        if (!route.isCached()) {
            AbstractRoute.addRouteToCache(start, route);
        }

        return route;
    }

    public static void startItemsRoute(Block start, ItemCollection collection) {
        setupRoute(start, itemsRouteFactory)
                .start(collection);
    }

    public static void startSignalRoute(Block start, PowerCentral source) {
        setupRoute(start, signalRouteFactory)
                .start(source);
    }

    public static <R extends AbstractRoute<R, P>, P extends OutputEntry> R createNewRoute(Block start, RouteFactory<R, P> factory) {
        R route = factory.create();

        expandRoute(route, start);

        return route;
    }

    private static BlockVector[] getRelativeVecs(BlockVector vec) {
        BlockVector[] vecs = new BlockVector[6];

        int i = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (Math.abs(x) == 1 && Math.abs(y) == 1
                            || Math.abs(x) == 1 && Math.abs(z) == 1
                            || Math.abs(z) == 1 && Math.abs(y) == 1
                            || x == 0 && y == 0 && z == 0) {
                        continue; // can be simplified?
                    }
                    // we're left with the blocks that is directly next to the relative block

                    vecs[i++] = (BlockVector) new BlockVector(vec).add(new Vector(x, y, z));
                }
            }
        }

        return vecs;
    }

    public static void expandRoute(AbstractRoute<?, ?> route, Block from) {
        BlockVector fromVec = BlockUtil.getVec(from);
        Material fromMat = from.getType();

        // iterate over all blocks around this block
        for (BlockVector relVec : getRelativeVecs(fromVec)) {
            if (!route.has(relVec)) {
                Block rel = BlockUtil.getBlock(from.getWorld(), relVec);
                route.search(from, fromMat, relVec, rel);
            }
        }
    }

    public static void updateNearbyRoutes(Block block) {
        BlockVector fromVec = BlockUtil.getVec(block);

        // check blocks in next tick, because we are calling this in a block/break event
        Bukkit.getScheduler().runTask(Factories.get(), () -> {
            // iterate over all blocks around this block
            for (BlockVector relVec : getRelativeVecs(fromVec)) {

                AbstractRoute<?, ?> route = AbstractRoute.getCachedRoute(block.getWorld(), relVec);
                if (route != null) {
                    if (block.getType() == Material.AIR) {
                        // the route was broken
                        AbstractRoute.removeRouteFromCache(block.getWorld(), route);
                        setupRoute(BlockUtil.getRel(block.getLocation(), relVec).getBlock(), route.getFactory());
                    } else {
                        // the route was expanded
                        expandRoute(route, block);
                        break;
                    }
                }
            }
        });
    }
}
