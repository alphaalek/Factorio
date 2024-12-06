package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public abstract class Circuit<M extends Mechanic<M>, C extends TransferCollection> extends AbstractMechanic<M> implements Container<C> {

    private boolean calledInput;

    public Circuit(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public void pipePut(C collection, PipePutEvent event) {
        if (calledInput) { // ensure no loop
            return;
        }

        calledInput = true;
        if (pipePut(collection)) {
            event.setTransferred(true);
        }
        calledInput = false;
    }

    public abstract boolean pipePut(C collection);
}
