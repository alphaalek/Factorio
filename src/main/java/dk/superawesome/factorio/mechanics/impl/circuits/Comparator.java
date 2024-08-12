package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.api.events.MechanicBuildEvent;
import dk.superawesome.factorio.api.events.MechanicRemoveEvent;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;

public class Comparator extends SignalTrigger<Comparator> implements ThinkingMechanic {

    private TransferCollection collectionTrigger;

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
