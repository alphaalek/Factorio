package dk.superawesome.factorio.mechanics.impl.power;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.api.events.MechanicLoadEvent;
import dk.superawesome.factorio.api.events.MechanicRemoveEvent;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.mechanics.routes.RouteFactory;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BlockVector;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class PowerLifter extends SignalTrigger<PowerLifter> implements SignalInvoker {

    private static final RouteFactory<PowerLifterRoute> ROUTE_FACTORY = new RouteFactory<>() {
        @Override
        public PowerLifterRoute create(BlockVector start, World world) {
            return new PowerLifterRoute(world, start);
        }

        @Override
        public EventHandler<PowerLifterRoute> getEventHandler() {
            return null;
        }
    };

    private boolean invoked;
    private boolean isRoot;
    private int poweredBy;

    public PowerLifter(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign) {
        super(loc, rotation, context, hasWallSign);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        setupRelativeBlocks();
    }

    @Override
    public MechanicProfile<PowerLifter> getProfile() {
        return Profiles.POWER_LIFTER;
    }

    private interface RouteOutput {

        void handle(Consumer<PowerLifter> andThen);
    }

    private static class PowerLifterRoute extends AbstractRoute<PowerLifterRoute, RouteOutput> {

        public PowerLifterRoute(World world, BlockVector start) {
            super(world, start);
        }

        @Override
        public RouteFactory<PowerLifterRoute> getFactory() {
            return ROUTE_FACTORY;
        }

        @Override
        public void search(Block from, BlockVector relVec, Block rel, boolean isFromOrigin) {
            if (rel.getType() == Material.OBSERVER
                    && Factorio.get().getMechanicManager(from.getWorld()).getMechanicAt(relVec) instanceof PowerLifter) {
                Block facing = BlockUtil.getPointingBlock(rel, true);
                if (isFromOrigin || !facing.equals(from)) {
                    addOutput(from.getWorld(), relVec, BlockUtil.getVec(from));
                    Routes.expandRoute(this, rel, from);
                }
            }
        }

        @Override
        protected RouteOutput createOutputEntry(World world, BlockVector vec, BlockVector from) {
            Mechanic<?> mechanic = Factorio.get().getMechanicManager(world).getMechanicAt(vec);
            if (mechanic instanceof PowerLifter lifter) {
                return andThen -> andThen.accept(lifter);
            }

            return andThen -> {};
        }

        public void start(Consumer<PowerLifter> andThen) {
            for (RouteOutput entry : getOutputs(Routes.DEFAULT_CONTEXT)) {
                entry.handle(andThen);
            }
        }
    }

    private void startLift(Consumer<PowerLifter> andThen) {
        Routes.setupRoute(this.loc.getBlock(), ROUTE_FACTORY, false)
                .start(andThen);
    }

    @Override
    public boolean invoke(SignalSource source) {
        if (invoked) {
            return false;
        }

        AtomicBoolean transferred = new AtomicBoolean();
        startLift(lifter -> {
            boolean did = lifter.invokeChild(source);
            if (did) {
                transferred.set(true);
            }
        });

        return transferred.get();
    }

    public boolean invokeChild(SignalSource source) {
        if (invoked) {
            return false;
        }

        boolean transferred = false;
        invoked = true;
        for (Block lever : levers) {
            boolean did = Routes.startSignalRoute(lever, source, false, false);
            if (did) {
                transferred = true;
            }
        }
        invoked = false;

        return transferred;
    }

    @EventHandler
    public void onSignal(BlockRedstoneEvent event) {
        if (BlockUtil.isDiagonal2DFast(event.getBlock(), loc.getBlock()) && event.getBlock().getType() == Material.REPEATER) {
            Block point = BlockUtil.getPointingBlock(event.getBlock(), true);
            if (point != null && point.getType() == Material.STICKY_PISTON && BlockUtil.getPointingBlock(point, false).equals(loc.getBlock())) {
                boolean prev = powered;
                double xDiff = this.loc.getX() - event.getBlock().getX();
                double zDiff = this.loc.getZ() - event.getBlock().getZ();
                // mask relative signals
                if (xDiff > 0 && zDiff == 0) {
                    if (event.getNewCurrent() > 0) {
                        poweredBy |= 1;
                    } else {
                        poweredBy &= ~1;
                    }
                } else if (xDiff < 0 && zDiff == 0) {
                    if (event.getNewCurrent() > 0) {
                        poweredBy |= 2;
                    } else {
                        poweredBy &= ~2;
                    }
                } else if (zDiff > 0 && xDiff == 0) {
                    if (event.getNewCurrent() > 0) {
                        poweredBy |= 4;
                    } else {
                        poweredBy &= ~4;
                    }
                } else if (zDiff < 0 && xDiff == 0) {
                    if (event.getNewCurrent() > 0) {
                        poweredBy |= 8;
                    } else {
                        poweredBy &= ~8;
                    }
                }
                powered = poweredBy > 0;

                if (powered != prev) {
                    startLift(lifter -> lifter.triggerLevers(powered));
                }
            }
        }
    }

    private void invokeRoot() {
        isRoot = false;
        powered = false;
        poweredBy = 0; // only used for root anyway, so we can clear it without any consequences
        BlockUtil.forRelative(this.loc.getBlock(), b -> {
            if (b.getType() == Material.STICKY_PISTON && BlockUtil.getPointingBlock(b, false).equals(this.loc.getBlock())) {
                BlockUtil.forRelative(b, b2 -> {
                    if (b2.getType() == Material.REPEATER && BlockUtil.getPointingBlock(b2, true).equals(b) && ((Powerable)b2.getBlockData()).isPowered()) {
                        isRoot = true;
                        double xDiff = this.loc.getX() - b2.getX();
                        double zDiff = this.loc.getZ() - b2.getZ();
                        // mask relative signals
                        if (xDiff > 0 && zDiff == 0) {
                            poweredBy |= 1;
                        } else if (xDiff < 0 && zDiff == 0) {
                            poweredBy |= 2;
                        } else if (zDiff > 0 && xDiff == 0) {
                            poweredBy |= 4;
                        } else if (zDiff < 0 && xDiff == 0) {
                            poweredBy |= 8;
                        }
                    }
                });
            }
        });

        if (isRoot) {
            powered = poweredBy > 0;
            startLift(lifter -> lifter.triggerLevers(powered));
        }
    }

    @EventHandler
    public void onMechanicRemove(MechanicRemoveEvent event) {
        if (event.getMechanic() == this) {
            // set levers off
            triggerLevers(false);

            // because the route is removed before the remove event is called, we have to make the first route expand manually
            Mechanic<?> mechanic = Factorio.get().getMechanicManagerFor(this).getMechanicAt(BlockUtil.getPointingBlock(this.loc.getBlock(), true).getLocation());
            if (mechanic instanceof PowerLifter lifter) {
                lifter.startLift(lifterChild -> lifterChild.triggerLevers(false));
            }
        }
    }

    @EventHandler
    public void onMechanicLoad(MechanicLoadEvent event) {
        if (event.getMechanic() instanceof PowerLifter) {
            invokeRoot();
        }
    }

    @EventHandler
    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        super.handleBlockBreak(event);

        if (BlockUtil.isRelativeFast(this.loc.getBlock(), event.getBlock()) || BlockUtil.isDiagonalYFast(this.loc.getBlock(), event.getBlock())) {
            Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                if (exists) {
                    invokeRoot();
                }
            });
        }
    }

    @EventHandler
    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        super.handleBlockPlace(event);
    }

    @EventHandler
    @Override
    public void onLeverPull(PlayerInteractEvent event) {
        super.handleLeverPull(event);
    }
}
