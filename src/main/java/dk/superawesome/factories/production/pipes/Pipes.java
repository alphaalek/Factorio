package dk.superawesome.factories.production.pipes;

import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.production.pipes.events.PipeSuckEvent;
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

        Block from = BlockUtil.getPointingBlock(start);
        if (from == null) {
            return;
        }

        PipeSuckEvent event = new PipeSuckEvent(from);
        Bukkit.getPluginManager().callEvent(event);
        if (event.getItems() == null) {
            // check world containers
            BlockState state = from.getState();
            if (state instanceof Container) {
                Container container = (Container) state;
                event.setItems(ItemCollection.from(container.getInventory()));
            }

            if (event.getItems() == null) {
                return;
            }
        }

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

        route.start(start, collection);
    }

    public static PipeRoute createNewRoute(Block start) {
        PipeRoute route = new PipeRoute();

        expandRoute(route, start);

        return route;
    }

    public static void expandRoute(PipeRoute route, Block from) {
        BlockVector fromVec = BlockUtil.getVec(from);
        Material fromMat = from.getType();
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    if (Math.abs(x) == 1 && Math.abs(y) == 1
                            || Math.abs(x) == 1 && Math.abs(z) == 1
                            || Math.abs(z) == 1 && Math.abs(y) == 1) {
                        continue;
                    }

                    BlockVector relVec = (BlockVector) fromVec.add(new Vector(x, y, z));
                    if (route.hasLocation(relVec)) {
                        continue;
                    }

                    Block rel = from.getRelative(x, y, z);
                    Material mat = rel.getType();
                    if (BlockUtil.piston.is(mat, (byte) 0)) {
                        route.addOutputLocation(from.getWorld(), fromVec, relVec);
                    } else if (
                            mat == Material.GLASS
                            ||
                                BlockUtil.anyStainedGlass.is(mat, BlockUtil.getData(rel))
                                && (fromMat == mat
                                    || fromMat == Material.GLASS
                                    || BlockUtil.stickyPiston.is(fromMat, BlockUtil.getData(from))
                                )
                    ) {
                        route.addLocation(from.getWorld(), fromVec, relVec);
                        expandRoute(route, rel);
                    }
                }
            }
        }
    }
}
