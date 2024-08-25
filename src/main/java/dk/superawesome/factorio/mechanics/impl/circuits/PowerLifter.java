package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.behaviour.PowerCentral;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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

    public PowerLifter(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        setupRelativeBlocks(__ -> {}, __ -> {});
    }

    @Override
    public MechanicProfile<PowerLifter> getProfile() {
        return Profiles.POWER_LIFTER;
    }

    @Override
    public boolean invoke(PowerCentral source) {
        AtomicBoolean transferred = new AtomicBoolean();

        for (Block lever : levers) {
            boolean did = Routes.startSignalRoute(lever, source, false);
            if (did) {
                transferred.set(true);
            }
        }

        startLift(l -> {
            boolean did = l.invoke(source);
            if (did) {
                transferred.set(true);
            }
        });

        if (transferred.get()) {
            powered = true;
            return true;
        }

        return false;
    }

    @EventHandler
    public void onSignal(BlockRedstoneEvent event) {
        if (event.getBlock().getType() == Material.REPEATER) {
            Block point = BlockUtil.getPointingBlock(event.getBlock(), true);
            if (point != null && point.getType() == Material.STICKY_PISTON && BlockUtil.getPointingBlock(point, false).equals(loc.getBlock())) {
                powered = event.getNewCurrent() > 0;
                triggerLevers(powered);
                startLift(l -> l.triggerLevers(powered));
            }
        }
    }

    private void startLift(Consumer<PowerLifter> andThen) {
        doLift(new HashSet<>(), andThen);
    }

    private void doLift(Set<BlockVector> route, Consumer<PowerLifter> andThen) {
        Block point = BlockUtil.getPointingBlock(loc.getBlock(), false);
        if (point != null) {
            BlockVector vec = BlockUtil.getVec(point);
            if (route.contains(vec)) {
                return;
            }

            route.add(vec);
            Mechanic<?> at = Factorio.get().getMechanicManagerFor(this).getMechanicAt(point.getLocation());
            if (at instanceof PowerLifter lifter) {
                andThen.accept(lifter);
                lifter.doLift(route, andThen);
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
    }

    @EventHandler
    @Override
    public void onLeverPull(PlayerInteractEvent event) {
        super.handleLeverPull(event);
    }
}
