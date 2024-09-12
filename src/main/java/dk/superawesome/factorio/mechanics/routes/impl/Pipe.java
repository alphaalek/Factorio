package dk.superawesome.factorio.mechanics.routes.impl;

import dk.superawesome.factorio.mechanics.Source;
import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.mechanics.routes.RouteFactory;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;

import java.util.LinkedList;

public class Pipe extends AbstractRoute<Pipe, Pipe.TransferOutputEntry> {

    public static class TransferOutputEntry {

        protected final Block block;

        private TransferOutputEntry(World world, BlockVector vec) {
            this.block = BlockUtil.getBlock(world, vec);
        }

        public boolean handle(TransferCollection collection, Source from) {
            PipePutEvent event = new PipePutEvent(block, collection, from);
            Bukkit.getPluginManager().callEvent(event);

            return event.transferred();
        }
    }

    public Pipe(BlockVector start, World world) {
        super(world, start);
    }

    @Override
    public RouteFactory<Pipe> getFactory() {
        return new RouteFactory.PipeRouteFactory();
    }

    @Override
    public void search(Block from, BlockVector relVec, Block rel, boolean isFromOrigin) {
        Material mat = rel.getType();

        // piston = pipe output
        if (mat == Material.PISTON) {
            add(relVec);
            Block point = BlockUtil.getPointingBlock(rel, false);
            // ... however only if the piston is not pointing towards the block where the pipe search came from
            if (!point.equals(from)) {
                add(BlockUtil.getVec(point));
                addOutput(from.getWorld(), BlockUtil.getVec(point), BlockUtil.getVec(rel));
            }
            // glass = pipe expand
        } else if (
                mat == Material.GLASS
                        || BlockUtil.anyStainedGlass.test(mat)
                        && (isFromOrigin
                        || from.getType() == mat
                        || from.getType() == Material.GLASS
                )
        ) {
            add(relVec);
            Routes.expandRoute(this, rel, from);
        }
    }

    @Override
    protected TransferOutputEntry createOutputEntry(World world, BlockVector vec, BlockVector from) {
        return new TransferOutputEntry(world, vec);
    }

    public boolean start(TransferCollection collection, Source from) {
        boolean transferred = false;
        for (TransferOutputEntry entry : outputs.get(Routes.DEFAULT_CONTEXT, LinkedList::new)) {
            if (entry.handle(collection, from)) {
                transferred = true;
            }

            if (collection.isTransferEmpty()) {
                break;
            }
        }

        return transferred;
    }
}
