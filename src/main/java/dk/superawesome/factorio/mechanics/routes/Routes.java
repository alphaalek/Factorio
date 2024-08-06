package dk.superawesome.factorio.mechanics.routes;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.SignalSource;
import dk.superawesome.factorio.mechanics.impl.Generator;
import dk.superawesome.factorio.mechanics.impl.PowerCentral;
import dk.superawesome.factorio.mechanics.routes.events.PipeSuckEvent;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Optional;

public class Routes {

    private static final RouteFactory<AbstractRoute.Pipe> transferRouteFactory = new RouteFactory.PipeRouteFactory();
    private static final RouteFactory<AbstractRoute.Signal> signalRouteFactory = new RouteFactory.SignalRouteFactory();

    public static boolean suckItems(Block start, PowerCentral source) {
        if (start.getType() != Material.STICKY_PISTON) {
            return false;
        }

        // we will suck items out of the mechanic that the sticky piston is pointing towards
        Block from = BlockUtil.getPointingBlock(start, false);
        if (from == null) {
            return false;
        }

        PipeSuckEvent event = new PipeSuckEvent(from);
        Bukkit.getPluginManager().callEvent(event);
        if (event.getTransfer() == null
                || event.getTransfer().isTransferEmpty()
                || source.getEnergy() < event.getTransfer().getTransferEnergyCost()) {
            return false;
        }

        source.setEnergy(source.getEnergy() - event.getTransfer().getTransferEnergyCost());

        // start the pipe route
        return startTransferRoute(start, event.getTransfer());
    }

    public static boolean transferEnergyToPowerCentral(Block start, Generator source) {
        Mechanic<?> mechanic = Factorio.get().getMechanicManager(start.getWorld()).getMechanicPartially(start.getLocation());
        if (!(mechanic instanceof PowerCentral powerCentral)) {
            return false;
        }

        // check if the power central has capacity for more energy
        if (powerCentral.getCapacity() == powerCentral.getEnergy()) {
            return false;
        }

        // transfer energy
        double take = powerCentral.getCapacity() - powerCentral.getEnergy();
        double amount = source.takeEnergy(take);
        powerCentral.setEnergy(powerCentral.getEnergy() + amount);

        return true;
    }

    private static <R extends AbstractRoute<R, P>, P extends OutputEntry> R setupRoute(Block start, RouteFactory<R> factory) {
        R route = AbstractRoute.getCachedRoute(start.getWorld(), BlockUtil.getVec(start));
        if (route == null) {
            route = createNewRoute(start, factory);
            AbstractRoute.addRouteToCache(start, route);
        }

        return route;
    }

    public static boolean startTransferRoute(Block start, TransferCollection collection) {
        return setupRoute(start, transferRouteFactory)
                .start(collection);
    }

    public static boolean startSignalRoute(Block start, SignalSource source) {
        return setupRoute(start, signalRouteFactory)
                .start(source);
    }

    public static void unloadRoutes(Chunk chunk) {
        for (AbstractRoute<?, ?> route : new ArrayList<>(AbstractRoute.getCachedRoutes(chunk.getWorld()))) {
            route.unload(chunk);
        }

        // TODO: load again
    }

    public static <R extends AbstractRoute<R, P>, P extends OutputEntry> R createNewRoute(Block start, RouteFactory<R> factory) {
        R route = factory.create(BlockUtil.getVec(start));
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

        // search origin vector
        if (!route.has(fromVec)) {
            route.search(from, fromMat, fromVec, from);
        }
        // iterate over all blocks around this block
        for (BlockVector relVec : getRelativeVecs(fromVec)) {
            // search relative vector
            if (!route.has(relVec)) {
                Block rel = BlockUtil.getBlock(from.getWorld(), relVec);
                route.search(from, fromMat, relVec, rel);
            }
        }
    }

    public static void updateNearbyRoutes(Block block) {
        BlockVector fromVec = BlockUtil.getVec(block);

        // check blocks in next tick, because we are calling this in a break/place event
        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
            AbstractRoute<?, ?> route = AbstractRoute.getCachedRoute(block.getWorld(), BlockUtil.getVec(block));
            if (route != null && block.getType() == Material.AIR) {
                // the route was broken, remove it from cache
                AbstractRoute.removeRouteFromCache(block.getWorld(), route);
            }

            // iterate over all blocks around this block
            for (BlockVector relVec : getRelativeVecs(fromVec)) {
                AbstractRoute<?, ?> relRoute = AbstractRoute.getCachedRoute(block.getWorld(), relVec);

                if (relRoute != null) {
                    AbstractRoute.removeRouteFromCache(block.getWorld(), relRoute);
                    route = relRoute;
                }
            }

            // setup again and connect routes
            if (route != null) {
                setupRoute(BlockUtil.getBlock(block.getWorld(), fromVec), route.getFactory());
            }
        });
    }
}
