package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.Set;

public abstract class Circuit<M extends Mechanic<M>, C extends TransferCollection> extends AbstractMechanic<M> implements Container<C> {

    private boolean calledInput;

    public Circuit(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public void pipePut(C collection, Set<AbstractRoute.Pipe> route, PipePutEvent event) {
        if (calledInput) {
            // remove all routes that the transfer went through, cause the route outputs were not excluded correctly
            // just let it build itself for the next transfer
            for (AbstractRoute.Pipe pipe : route) {
                AbstractRoute.removeRouteFromCache(pipe);
            }
            return;
        }

        calledInput = true;
        if (pipePut(collection, route)) {
            event.setTransferred(true);
        }
        calledInput = false;
    }

    public abstract boolean pipePut(C collection, Set<AbstractRoute.Pipe> route);
}