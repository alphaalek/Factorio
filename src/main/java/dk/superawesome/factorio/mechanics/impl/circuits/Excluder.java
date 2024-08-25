package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.mechanics.Circuit;
import dk.superawesome.factorio.mechanics.MechanicProfile;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.mechanics.Profiles;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class Excluder extends Circuit<Excluder, ItemCollection> implements ItemContainer {

    public Excluder(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public MechanicProfile<Excluder> getProfile() {
        return Profiles.EXCLUDER;
    }

    @Override
    public boolean pipePut(ItemCollection collection) {
        return false;
    }

    @Override
    public boolean isContainerEmpty() {
        return true;
    }

    @Override
    public int getCapacity() {
        return -1;
    }
}
