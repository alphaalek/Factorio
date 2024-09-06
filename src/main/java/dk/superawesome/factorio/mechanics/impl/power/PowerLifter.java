package dk.superawesome.factorio.mechanics.impl.power;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BlockVector;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class PowerLifter extends SignalTrigger<PowerLifter> implements SignalInvoker {

    private boolean invoked;

    public PowerLifter(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        setupRelativeBlocks();
    }

    @Override
    public MechanicProfile<PowerLifter> getProfile() {
        return Profiles.POWER_LIFTER;
    }

    @Override
    public boolean invoke(SignalSource source) {
        if (invoked) {
            return false;
        }

        AtomicBoolean transferred = new AtomicBoolean();
        startLift(l -> {
            boolean did = l.invokeChild(source);
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

        if (transferred) {
            triggerLevers(true);
            return true;
        }

        return false;
    }

    @EventHandler
    public void onSignal(BlockRedstoneEvent event) {
        if (BlockUtil.isDiagonalYFast(event.getBlock(), loc.getBlock()) && event.getBlock().getType() == Material.REPEATER) {
            Block point = BlockUtil.getPointingBlock(event.getBlock(), true);
            if (point != null && point.getType() == Material.STICKY_PISTON && BlockUtil.getPointingBlock(point, false).equals(loc.getBlock())) {
                powered = event.getNewCurrent() > 0;

                startLift(l -> l.triggerLevers(powered));
            }
        }
    }

    private void startLift(Consumer<PowerLifter> andThen) {
        andThen.accept(this);
        doLift(null, new HashSet<>(), andThen);
    }

    private void doLift(Block from, Set<BlockVector> route, Consumer<PowerLifter> andThen) {
        Block point = BlockUtil.getPointingBlock(loc.getBlock(), true);
        if (point != null && point.getType() == Material.OBSERVER && (from == null || !BlockUtil.getPointingBlock(point, true).equals(from))) {
            BlockVector vec = BlockUtil.getVec(point);
            if (route.contains(vec)) {
                return;
            }

            route.add(vec);
            Mechanic<?> at = Factorio.get().getMechanicManagerFor(this).getMechanicAt(point.getLocation());
            if (at instanceof PowerLifter lifter) {
                andThen.accept(lifter);
                lifter.doLift(point, route, andThen);
            }
        }
    }

    @EventHandler
    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        super.handleBlockPlace(event);
    }

    @EventHandler
    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        super.handleBlockBreak(event);

        if (BlockUtil.isDiagonal2DFast(event.getBlock(), loc.getBlock())) {
            Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                boolean prevPowered = powered;
                powered = false;
                BlockUtil.forRelative(loc.getBlock(), b -> {
                    if (b.getType() == Material.STICKY_PISTON && BlockUtil.getPointingBlock(b, false).equals(loc.getBlock())) {
                        BlockUtil.forRelative(b, b2 -> {
                            if (b2.getType() == Material.REPEATER && BlockUtil.getPointingBlock(b2, true).equals(b) && ((Repeater)b2.getBlockData()).isPowered()) {
                                powered = true;
                            }
                        });
                    // TODO: Is this really needed? And might it even show it's powered without actually being it sometimes?
                    } else if (b.getType() == Material.OBSERVER && BlockUtil.getPointingBlock(b, true).equals(loc.getBlock())) {
                        if (Factorio.get().getMechanicManager(b.getWorld()).getMechanicAt(b.getLocation()) instanceof PowerLifter lifter) {
                            powered = lifter.powered;
                        }
                    }
                });

                if (prevPowered != powered) {
                    startLift(p -> p.triggerLevers(powered));
                }
            });
        }
    }

    @EventHandler
    @Override
    public void onLeverPull(PlayerInteractEvent event) {
        super.handleLeverPull(event);
    }
}
