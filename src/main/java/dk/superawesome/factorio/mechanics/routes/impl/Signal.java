package dk.superawesome.factorio.mechanics.routes.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.SignalInvoker;
import dk.superawesome.factorio.mechanics.SignalSource;
import dk.superawesome.factorio.mechanics.impl.power.PowerCentral;
import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.mechanics.routes.RouteFactory;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BlockVector;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Signal extends AbstractRoute<Signal, Signal.SignalOutputEntry> {

    public static class SignalOutputEntry {

        protected final Block block;
        protected final Location loc;
        protected final Block from;

        private SignalOutputEntry(World world, BlockVector vec, BlockVector from) {
            this.block = BlockUtil.getBlock(world, vec);
            this.loc = block.getLocation();
            this.from = BlockUtil.getBlock(world, from);
        }

        public boolean handle(SignalSource source) {
            return source.handleOutput(block, loc, from);
        }
    }

    private final Map<BlockVector, Integer> signals = new HashMap<>();

    public Signal(BlockVector start, World world) {
        super(world, start);
    }

    @Override
    public RouteFactory<Signal> getFactory() {
        return RouteFactory.SignalRouteFactory.FACTORY;
    }

    @Override
    public void search(Block from, BlockVector relVec, Block rel, boolean isFromOrigin) {
        int signal = signals.getOrDefault(BlockUtil.getVec(from), 16);
        Material mat = rel.getType();
        if (mat == Material.REPEATER && BlockUtil.getPointingBlock(rel, false).equals(from)) {
            add(relVec);

            Block facing = BlockUtil.getPointingBlock(rel, true);
            // facing sticky piston - signal output
            if (facing.getType() == Material.STICKY_PISTON) {
                add(BlockUtil.getVec(facing));

                Block facingFacing = BlockUtil.getPointingBlock(facing, false);
                Mechanic<?> mechanic = Factorio.get().getMechanicManager(rel.getWorld()).getMechanicAt(facingFacing.getLocation());
                if (mechanic instanceof SignalInvoker) {
                    addOutput(from.getWorld(), BlockUtil.getVec(facingFacing), BlockUtil.getVec(facing), SignalSource.TO_POWER_CENTRAL);
                }
                if (mechanic != null) {
                    addOutput(from.getWorld(), BlockUtil.getVec(facingFacing), BlockUtil.getVec(facing), SignalSource.FROM_POWER_CENTRAL);
                }

                return;
            }

            if (!expandWire(facing, rel, rel, 16)
                    && facing.getType().isSolid() && facing.getType().isOccluding()) {
                signals.put(relVec, 16);
                Routes.expandRoute(this, rel, relVec, BlockUtil.getFacing(rel).getOppositeFace());
            }

            // comparator - signal output
        } else if (mat == Material.COMPARATOR && BlockUtil.getPointingBlock(rel, false).equals(from)) {
            add(relVec);

            Block facing = BlockUtil.getPointingBlock(rel, true);
            Mechanic<?> mechanic = Factorio.get().getMechanicManager(rel.getWorld()).getMechanicAt(facing.getLocation());
            if (mechanic instanceof PowerCentral) {
                addOutput(rel.getWorld(), BlockUtil.getVec(facing), BlockUtil.getVec(rel), SignalSource.TO_POWER_CENTRAL);
            }

            expandWire(facing, rel, rel, 16);
            // check for expand signal route
        } else if (signal > 1) {
            if (mat == Material.REDSTONE_WIRE) {
                expandWire(rel, relVec, from, signal - 1);
                return;
            }

            if (from.getType() == Material.REPEATER && mat.isSolid() && mat.isOccluding()) {
                add(relVec);
                for (BlockFace face : Routes.SIGNAL_EXPAND_DIRECTIONS) {
                    Block sourceRel = rel.getRelative(face);
                    if (!sourceRel.equals(from)) {
                        expandWire(sourceRel, rel, rel, 16);
                    }
                }
            }

            Block up = rel.getRelative(BlockFace.UP);
            Block down = rel.getRelative(BlockFace.DOWN);

            Block insulatorUp = from.getRelative(BlockFace.UP);
            Block insulatorDown = from.getRelative(BlockFace.DOWN);

            if (up.getType() == Material.REDSTONE_WIRE
                    && (from.getType() == Material.REDSTONE_WIRE && !insulatorUp.getType().isSolid() && !insulatorUp.getType().isOccluding()
                    || from.getType() == Material.REPEATER && BlockUtil.getPointingBlock(from, true).equals(rel)
            )
            ) {
                expandWire(up, insulatorUp, rel, signal - 1);
            }

            if (from.getType() == Material.REDSTONE_WIRE && insulatorDown.getType().isSolid() && insulatorDown.getType().isOccluding()
                    && (down.getType() == Material.REDSTONE_WIRE && !mat.isSolid() && !mat.isOccluding()
                    || down.getType() == Material.REPEATER && BlockUtil.getPointingBlock(down, false).equals(insulatorDown))) {
                expandWire(down, insulatorDown, insulatorDown, signal - 1);
            } else if (from.getType() == Material.REPEATER
                    && mat.isSolid() && mat.isOccluding()
                    && down.getType() == Material.REDSTONE_WIRE) {
                expandWire(down, BlockUtil.getVec(down), insulatorDown, signal - 1);
            }
        }
    }

    private boolean expandWire(Block block, Block ignore, Block point, int signal) {
        add(BlockUtil.getVec(block));
        if (block.getType() == Material.REDSTONE_WIRE) {
            expandWire(block, BlockUtil.getVec(block), ignore, signal);
            return true;
        } else if ((block.getType() == Material.REPEATER || block.getType() == Material.COMPARATOR)
                && BlockUtil.getPointingBlock(block, false).equals(point)) {
            signals.put(BlockUtil.getVec(block), 16);
            Routes.expandRoute(this, block, BlockUtil.getVec(block), BlockUtil.getFacing(block).getOppositeFace());
            return true;
        }

        return false;
    }

    private void expandWire(Block rel, BlockVector relVec, Block ignore, int signal) {
        add(relVec);
        signals.put(relVec, signal);
        Routes.expandRoute(this, rel, ignore);
    }

    @Override
    protected SignalOutputEntry createOutputEntry(World world, BlockVector vec, BlockVector from) {
        return new SignalOutputEntry(world, vec, from);
    }

    public boolean start(SignalSource source, boolean firstCall) {
        if (!source.preSignal(this, firstCall)) {
            return false;
        }

        // handle signal outputs
        int mechanics = 0;
        for (SignalOutputEntry entry : getOutputs(source.getContext())) {
            if (entry.handle(source)) {
                mechanics++;
            }
        }

        // handle power related mechanic stress
        if (getOutputs(source.getContext()).isEmpty() || mechanics < getOutputs(source.getContext()).size()) {
            source.postSignal(this, mechanics);
        }

        return mechanics > 0;
    }
}
