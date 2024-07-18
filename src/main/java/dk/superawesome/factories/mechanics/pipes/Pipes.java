package dk.superawesome.factories.mechanics.pipes;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.mechanics.ItemCollection;
import dk.superawesome.factories.mechanics.pipes.events.PipeSuckEvent;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

public class Pipes {

    public static void suckItems(Block start) {
        if (!BlockUtil.stickyPiston.test(start.getType())) {
            return;
        }

        // we will suck items out of the block that the sticky piston is pointing towards
        // if it's neither a mechanic
        Block from = BlockUtil.getPointingBlock(start, false);
        if (from == null) {
            return;
        }

        PipeSuckEvent event = new PipeSuckEvent(from);
        Bukkit.getPluginManager().callEvent(event);
        if (event.getItems() == null || event.getItems().isEmpty()) {
            return;
        }

        // start the pipe route
        startRoute(start, event.getItems());
    }

    public static void startRoute(Block start, ItemCollection collection) {
        PipeRoute route = PipeRoute.getCachedRoute(start.getWorld(), BlockUtil.getVec(start));
        if (route == null) {
            route = createNewRoute(start);
        }

        if (!route.isCached()) {
            PipeRoute.addRouteToCache(start.getWorld(), route);
        }

        route.start(collection);
    }

    public static PipeRoute createNewRoute(Block start) {
        PipeRoute route = new PipeRoute();

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

    public static void expandRoute(PipeRoute route, Block from) {
        BlockVector fromVec = BlockUtil.getVec(from);
        Material fromMat = from.getType();

        // iterate over all blocks around this block
        for (BlockVector relVec : getRelativeVecs(fromVec)) {
            if (!route.has(relVec)) {
                Block rel = BlockUtil.getBlock(from.getWorld(), relVec);
                Material mat = rel.getType();

                // piston = pipe output
                if (BlockUtil.piston.test(mat)) {
                    route.addOutput(from.getWorld(), relVec);
                // glass = pipe expand
                } else if (
                        mat == Material.GLASS
                                ||
                                BlockUtil.anyStainedGlass.test(mat)
                                        && (fromMat == mat
                                        || fromMat == Material.GLASS
                                        || BlockUtil.stickyPiston.test(fromMat)
                                )
                ) {
                    route.add(relVec);
                    expandRoute(route, rel);
                }
            }
        }
    }

    public static void updateNearbyRoutes(Block block) {
        BlockVector fromVec = BlockUtil.getVec(block);

        // check blocks in next tick, because we are calling this in a block/break event
        Bukkit.getScheduler().runTask(Factories.get(), () -> {
            // iterate over all blocks around this block
            for (BlockVector relVec : getRelativeVecs(fromVec)) {

                PipeRoute route = PipeRoute.getCachedRoute(block.getWorld(), relVec);
                if (route != null) {
                    if (block.getType() == Material.AIR) {
                        // the pipe was broken
                        route.clear();
                        expandRoute(route, BlockUtil.getBlock(block.getWorld(), relVec));
                    } else {
                        // the pipe was expanded
                        expandRoute(route, block);
                    }
                }
            }
        });
    }
}
