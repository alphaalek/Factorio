package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.api.events.MechanicBuildEvent;
import dk.superawesome.factorio.api.events.MechanicRemoveEvent;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.List;

public class Comparator extends SignalTrigger<Comparator> implements ThinkingMechanic {

    private final List<Block> levers = new ArrayList<>();

    private TransferCollection collectionTrigger;
    private boolean powered;

    public Comparator(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
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

        for (Block lever : levers) {
            triggerLever(lever, powered);
        }
    }

    @Override
    public void onBlocksLoaded() {
        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
            MechanicManager manager = Factorio.get().getMechanicManagerFor(this);
            for (BlockFace face : Routes.RELATIVES) {
                Block block = loc.getBlock().getRelative(face);
                if (block.getType() == Material.LEVER) {
                    levers.add(block);
                    triggerLever(block, false);
                }

                Mechanic<?> at = manager.getMechanicPartially(block.getLocation());
                if (at instanceof TransferCollection collection) {
                    this.collectionTrigger = collection;
                }
            }
        });
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
            if (Factorio.get().getMechanicManagerFor(this).getMechanicPartially(
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
