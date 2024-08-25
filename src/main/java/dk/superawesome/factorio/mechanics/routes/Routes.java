package dk.superawesome.factorio.mechanics.routes;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.power.PowerCentral;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipeSuckEvent;
import dk.superawesome.factorio.mechanics.transfer.EnergyCollection;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BlockVector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Routes {

    public static final RouteFactory<AbstractRoute.Pipe> transferRouteFactory = new RouteFactory.PipeRouteFactory();
    public static final RouteFactory<AbstractRoute.Signal> signalRouteFactory = new RouteFactory.SignalRouteFactory();

    public static final int DEFAULT_CONTEXT = 0;
    public static final BlockFace[] SIGNAL_EXPAND_DIRECTIONS = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
    public static final BlockFace[] RELATIVES = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

    public static boolean invokeSignalOutput(Block block, Location loc, Block start, PowerCentral source) {
        Mechanic<?> at = Factorio.get().getMechanicManager(block.getWorld()).getMechanicAt(loc);
        if (at instanceof SignalInvoker invoker) {
            return invoker.invoke(source);
        }

        PipeSuckEvent event = new PipeSuckEvent(block);
        Bukkit.getPluginManager().callEvent(event);

        if (event.getTransfer() == null) {
            return false;
        }

        source.setRecentMax(source.getRecentMax() + event.getTransfer().getTransferEnergyCost());

        if (!event.getTransfer().getTransferDelayHandler().ready()
                || event.getTransfer().isTransferEmpty()
                || source.getEnergy() < event.getTransfer().getTransferEnergyCost()) {
            return false;
        }

        // start the pipe route
        if (startTransferRoute(start, event.getTransfer(), source, false)) {
            source.setEnergy(source.getEnergy() - event.getTransfer().getTransferEnergyCost());
            return true;
        }

        return false;
    }

    public static boolean transferEnergyToPowerCentral(Block start, Location loc, EnergyCollection source) {
        Mechanic<?> mechanic = Factorio.get().getMechanicManager(start.getWorld()).getMechanicPartially(loc);
        if (!(mechanic instanceof PowerCentral powerCentral)) {
            return false;
        }

        // check if the power central has capacity for more energy
        if (powerCentral.getCapacity() == powerCentral.getEnergy()) {
            return false;
        }

        // transfer energy
        double take = powerCentral.getCapacity() - powerCentral.getEnergy();
        double amount = source.take(take);
        powerCentral.setEnergy(powerCentral.getEnergy() + amount);

        return true;
    }

    public static <R extends AbstractRoute<R, P>, P extends OutputEntry> R setupRoute(Block start, RouteFactory<R> factory, boolean onlyExpandIfOriginValid) {
        R route = AbstractRoute.getCachedOriginRoute(start.getWorld(), BlockUtil.getVec(start));
        if (route == null) {
            route = createNewRoute(start, factory, onlyExpandIfOriginValid);
            AbstractRoute.addRouteToCache(route);
        }

        return route;
    }

    public static boolean startTransferRoute(Block start, TransferCollection collection, Source from, boolean onlyExpandIfOriginValid) {
        return setupRoute(start, transferRouteFactory, onlyExpandIfOriginValid)
                .start(collection, from);
    }

    public static boolean startSignalRoute(Block start, SignalSource source, boolean onlyExpandIfOriginValid) {
        return setupRoute(start, signalRouteFactory, onlyExpandIfOriginValid)
                .start(source);
    }

    public static <R extends AbstractRoute<R, P>, P extends OutputEntry> R createNewRoute(Block start, RouteFactory<R> factory, boolean onlyExpandIfOriginValid) {
        R route = factory.create(BlockUtil.getVec(start), start.getWorld());
        startRoute(route, start, onlyExpandIfOriginValid);

        return route;
    }

    public static void startRoute(AbstractRoute<?, ?> route, Block from, boolean onlyExpandIfOriginValid) {
        BlockVector fromVec = BlockUtil.getVec(from);

        // search origin vector
        if (!route.hasVisited(fromVec, fromVec) && !route.getLocations().contains(fromVec)) {
            route.visit(fromVec, fromVec);
            route.search(from, fromVec, from, true);

            // the origin vector was not added to the route, stop expanding
            if (!route.getLocations().contains(fromVec) && onlyExpandIfOriginValid) {
                return;
            }

            route.add(fromVec);
        }

        expandRoute(route, from, from, true);
    }

    public static void expandRoute(AbstractRoute<?, ?> route, Block from, Block ignore) {
        expandRoute(route, from, ignore, false);
    }

    public static void expandRoute(AbstractRoute<?, ?> route, Block from, Block ignore, boolean isFromOrigin) {
        BlockVector fromVec = BlockUtil.getVec(from);

        // iterate over all blocks around this block
        for (BlockFace face : route instanceof AbstractRoute.Signal ? SIGNAL_EXPAND_DIRECTIONS : RELATIVES) {
            BlockVector relVec = BlockUtil.getVec(BlockUtil.getRel(from.getLocation(), face.getDirection()));
            // search relative vector
            if (!route.hasVisited(fromVec, relVec)) {
                Block rel = BlockUtil.getBlock(from.getWorld(), relVec);
                if (rel.equals(ignore)) {
                    continue;
                }

                route.visit(fromVec, relVec);
                route.search(from, relVec, rel, isFromOrigin);
            }
        }
    }

    public static void expandRoute(AbstractRoute<?, ?> route, Block from, BlockVector fromVec, BlockFace face) {
        BlockVector relVec = BlockUtil.getVec(BlockUtil.getRel(from.getLocation(), face.getDirection()));

        if (!route.hasVisited(fromVec, relVec)) {
            Block rel = BlockUtil.getBlock(from.getWorld(), relVec);

            route.visit(fromVec, relVec);
            route.search(from, relVec, rel, false);
        }
    }

    public static <R extends AbstractRoute<R, P>, P extends OutputEntry> void setupForcibly(Block block, RouteFactory<R> factory, boolean onlyExpandIfOriginValid) {
        updateNearbyRoutes(block, true, modified -> {
            if (modified.isEmpty()) {
                setupRoute(block, factory, onlyExpandIfOriginValid);
            }
        });
    }

    public static void removeNearbyRoutes(Block block) {
        updateNearbyRoutes(block, false, null);
    }

    public static void removeNearbyRoutesSync(Block block) {
        updateNearbyRoutesSync(block, false, null);
    }

    public static void updateNearbyRoutesSync(Block block, boolean addAgain, Consumer<List<AbstractRoute<?, ?>>> modifiedRoutesFunction) {
        // check blocks in next tick, because we are calling this in a break/place event
        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
            updateNearbyRoutes(block, addAgain, modifiedRoutesFunction);
        });
    }

    public static void updateNearbyRoutes(Block block, boolean addAgain, Consumer<List<AbstractRoute<?, ?>>> modifiedRoutesFunction) {
        List<AbstractRoute<?, ?>> routes = new ArrayList<>(AbstractRoute.getCachedRoutes(block.getWorld(), BlockUtil.getVec(block)));
        for (AbstractRoute<?, ?> route : routes) {
            AbstractRoute.removeRouteFromCache(route);
        }

        List<AbstractRoute<?, ?>> modifiedRoutes = new ArrayList<>();
        modifiedRoutes.addAll(routes);
        // iterate over all blocks around this block
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                        // ignore corner blocks
                        continue;
                    }

                    BlockVector rel = BlockUtil.getVec(BlockUtil.getRel(block.getLocation(), new BlockVector(x, y, z)));
                    List<AbstractRoute<?, ?>> relRoutes = new ArrayList<>(AbstractRoute.getCachedRoutes(block.getWorld(), rel));

                    if (!relRoutes.isEmpty()) {
                        for (AbstractRoute<?, ?> relRoute : relRoutes) {
                            if (relRoute instanceof AbstractRoute.Pipe) {
                                // ignore edge blocks for pipes
                                if ((Math.abs(x) == 1 || Math.abs(z) == 1) && Math.abs(y) == 1) {
                                    continue;
                                }
                            } else if (relRoute instanceof AbstractRoute.Signal) {
                                // ignore up/down blocks for signal
                                if (x == 0 && z == 0 && Math.abs(y) == 1) {
                                    continue;
                                }
                            }

                            AbstractRoute.removeRouteFromCache(relRoute);
                            modifiedRoutes.add(relRoute);
                        }
                    }
                }
            }
        }

        if (addAgain) {
            // setup again and connect routes
            for (AbstractRoute<?, ?> modifiedRoute : modifiedRoutes) {
                setupRoute(BlockUtil.getBlock(block.getWorld(), modifiedRoute.getStart()), modifiedRoute.getFactory(), true);
            }
        }
        if (modifiedRoutesFunction != null) {
            modifiedRoutesFunction.accept(modifiedRoutes);
        }
    }
}
