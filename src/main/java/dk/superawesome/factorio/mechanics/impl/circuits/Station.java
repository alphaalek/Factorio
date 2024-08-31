package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class Station extends Circuit<Station, TransferCollection> implements Container<TransferCollection> {

    public Station(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public MechanicProfile<Station> getProfile() {
        return Profiles.STATION;
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
    public boolean pipePut(TransferCollection collection) {
        return Routes.startTransferRoute(loc.getBlock(), collection, this, false);
    }

    @Override
    public int getCapacity() {
        return -1;
    }
}
