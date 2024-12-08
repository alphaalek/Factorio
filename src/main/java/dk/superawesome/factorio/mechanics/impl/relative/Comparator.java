package dk.superawesome.factorio.mechanics.impl.relative;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.api.events.MechanicBuildEvent;
import dk.superawesome.factorio.api.events.MechanicRemoveEvent;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class Comparator extends SignalTrigger<Comparator> implements ThinkingMechanic {

    private TransferCollection collectionTrigger;

    public Comparator(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public MechanicProfile<Comparator> getProfile() {
        return Profiles.COMPARATOR;
    }

    @Override
    public void think() {
        if (collectionTrigger != null) {
            if (collectionTrigger.isTransferEmpty() && powered) {
                powered = false;
            } else if (!collectionTrigger.isTransferEmpty() && !powered) {
                powered = true;
            } else return;
        } else if (powered) {
            powered = false;
        } else return;

        triggerLevers();
    }

    @Override
    public void onBlocksLoaded(Player by) {
        this.collectionTrigger = null;
        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
            setupRelativeBlocks(at -> triggerLever(at, true), at -> {
                if (at instanceof TransferCollection collection) {
                    this.collectionTrigger = collection;
                }
            });

            think();
        });
    }

    @EventHandler
    @Override
    public void onLeverPull(PlayerInteractEvent event) {
        super.handleLeverPull(event);
    }

    @EventHandler
    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        super.handleBlockPlace(event);
    }

    @EventHandler
    @Override
    public void onBlockBreak(BlockBreakEvent event)  {
        super.handleBlockBreak(event);
    }

    @EventHandler
    public void onMechanicBuild(MechanicBuildEvent event) {
        if (this.collectionTrigger == null && event.getMechanic() instanceof TransferCollection collection) {
            if (Factorio.get().getMechanicManagerFor(this).getMechanicAt(
                    BlockUtil.getPointingBlock(loc.getBlock(), true).getLocation()) == collection) {
                this.collectionTrigger = collection;
            }
        }
    }

    @EventHandler
    public void onMechanicRemove(MechanicRemoveEvent event) {
        if (this.collectionTrigger == event.getMechanic()) {
            this.collectionTrigger = null;
        }
    }

    @Override
    public DelayHandler getThinkDelayHandler() {
        return DelayHandler.NO_DELAY;
    }
}
