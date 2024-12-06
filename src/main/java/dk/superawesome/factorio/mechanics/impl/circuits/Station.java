package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.mechanics.Circuit;
import dk.superawesome.factorio.mechanics.MechanicProfile;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.mechanics.Profiles;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class Station extends Circuit<Station, TransferCollection> implements Container<TransferCollection> {

    public Station(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
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
