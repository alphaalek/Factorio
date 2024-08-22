package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.mechanics.AbstractMechanic;
import dk.superawesome.factorio.mechanics.MechanicProfile;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.mechanics.Profiles;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class Station extends AbstractMechanic<Station> implements Container<TransferCollection> {

    public Station(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public MechanicProfile<Station> getProfile() {
        return Profiles.Station;
    }

    @Override
    public boolean accepts(TransferCollection collection) {
        return true;
    }

    @Override
    public boolean isContainerEmpty() {
        return true;
    }

    @Override
    public void pipePut(TransferCollection collection, PipePutEvent event) {
        if (Routes.startTransferRoute(loc.getBlock(), collection, false)) {
            event.setTransferred(true);
        }
    }

    @Override
    public int getCapacity() {
        return -1;
    }
}
