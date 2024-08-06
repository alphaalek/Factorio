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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Routes {

    private static final RouteFactory<AbstractRoute.Pipe> transferRouteFactory = new RouteFactory.PipeRouteFactory();
    private static final RouteFactory<AbstractRoute.Signal> signalRouteFactory = new RouteFactory.SignalRouteFactory();

    private static final BlockFace[] SIGNAL_EXPAND_DIRECTIONS = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

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
        R route = AbstractRoute.getCachedOriginRoute(start.getWorld(), BlockUtil.getVec(start));
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

    public static <R extends AbstractRoute<R, P>, P extends OutputEntry> R createNewRoute(Block start, RouteFactory<R> factory) {
        R route = factory.create(BlockUtil.getVec(start));
        expandRoute(route, start);

        return route;
    }

    public static void expandRoute(AbstractRoute<?, ?> route, Block from) {
        BlockVector fromVec = BlockUtil.getVec(from);
        Material fromMat = from.getType();

        // search origin vector
        if (!route.hasVisited(fromVec, fromVec) && !route.getLocations().contains(fromVec)) {
            route.visit(fromVec, fromVec);
            route.search(from, fromMat, fromVec, from);
        }
        // iterate over all blocks around this block
        for (BlockFace face : SIGNAL_EXPAND_DIRECTIONS) {
            BlockVector relVec = BlockUtil.getVec(BlockUtil.getRel(from.getLocation(), face.getDirection()));
            // search relative vector
            if (!route.hasVisited(fromVec, relVec) && !route.getLocations().contains(relVec)) {
                Block rel = BlockUtil.getBlock(from.getWorld(), relVec);
                route.visit(fromVec, relVec);
                route.search(from, fromMat, relVec, rel);
            }
        }
    }

    public static void updateNearbyRoutes(Block block) {
        // check blocks in next tick, because we are calling this in a break/place event
        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
            List<AbstractRoute<?, ?>> routes = new ArrayList<>(AbstractRoute.getCachedRoutes(block.getWorld(), BlockUtil.getVec(block)));
            if (!routes.isEmpty() && block.getType() == Material.AIR) {
                for (AbstractRoute<?, ?> route : routes) {
                    // the route was broken, remove it from cache
                    AbstractRoute.removeRouteFromCache(block.getWorld(), route);
                }
            }

            List<AbstractRoute<?, ?>> modifiedRoutes = new ArrayList<>(routes);
            // iterate over all blocks around this block
            for (BlockFace face : SIGNAL_EXPAND_DIRECTIONS) {
                for (BlockFace rel : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.SELF}) {
                    BlockVector relVec = BlockUtil.getVec(BlockUtil.getRel(block.getLocation(), face.getDirection().add(rel.getDirection())));
                    List<AbstractRoute<?, ?>> relRoutes = new ArrayList<>(AbstractRoute.getCachedRoutes(block.getWorld(), relVec));

                    if (!relRoutes.isEmpty()) {
                        for (AbstractRoute<?, ?> relRoute : relRoutes) {
                            AbstractRoute.removeRouteFromCache(block.getWorld(), relRoute);
                            modifiedRoutes.add(relRoute);
                        }
                    }
                }
            }

            // setup again and connect routes
            for (AbstractRoute<?, ?> modifiedRoute : modifiedRoutes) {
                setupRoute(BlockUtil.getBlock(block.getWorld(), modifiedRoute.getStart()), modifiedRoute.getFactory());
            }
        });
    }
}
