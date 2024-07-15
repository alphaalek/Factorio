package dk.superawesome.factories.mehcanics.pipes;

import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.mehcanics.pipes.events.PipeSuckEvent;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

public class Pipes {

    public static void suckItems(Block start) {
        if (!BlockUtil.stickyPiston.is(start)) {
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
        if (event.getItems() == null) {
            return;
        }

        // start the pipe route
        startRoute(start, event.getItems());
    }

    public static void startRoute(Block start, ItemCollection collection) {
        PipeRoute route = PipeRoute.getCachedRote(start);
        if (route == null) {
            route = createNewRoute(start);
        }

        if (!route.isCached()) {
            PipeRoute.addRouteToCache(route);
        }

        route.start(collection);
    }

    public static PipeRoute createNewRoute(Block start) {
        PipeRoute route = new PipeRoute();

        expandRoute(route, start);

        return route;
    }

    public static void expandRoute(PipeRoute route, Block from) {
        BlockVector fromVec = BlockUtil.getVec(from);
        Material fromMat = from.getType();

        // iterate over all blocks around this block
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (Math.abs(x) == 1 && Math.abs(y) == 1
                            || Math.abs(x) == 1 && Math.abs(z) == 1
                            || Math.abs(z) == 1 && Math.abs(y) == 1) {
                        continue;
                    }
                    // we're left with the blocks that is directly next to the relative block

                    // generate a world block-vector and check if the pipe route has already
                    // came across this vector in the search
                    BlockVector relVec = (BlockVector) new BlockVector(fromVec).add(new Vector(x, y, z));
                    if (route.has(relVec)) {
                        continue;
                    }

                    Block rel = from.getRelative(x, y, z);
                    Material mat = rel.getType();
                    // piston = pipe output
                    if (BlockUtil.piston.is(mat, BlockUtil.getData(rel))) {
                        route.addOutput(from.getWorld(), relVec);
                    // glass = pipe expand
                    } else if (
                            mat == Material.GLASS
                            ||
                                BlockUtil.anyStainedGlass.is(mat, BlockUtil.getData(rel))
                                && (fromMat == mat
                                    || fromMat == Material.GLASS
                                    || BlockUtil.stickyPiston.is(fromMat, BlockUtil.getData(from))
                                )
                    ) {
                        route.add(relVec);
                        expandRoute(route, rel);
                    }
                }
            }
        }
    }
}
