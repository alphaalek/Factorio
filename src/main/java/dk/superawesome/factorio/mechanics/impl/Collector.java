package dk.superawesome.factorio.mechanics.impl;

import dk.superawesome.factorio.mechanics.AbstractMechanic;
import dk.superawesome.factorio.mechanics.MechanicProfile;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.mechanics.Profiles;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class Collector extends AbstractMechanic<Collector> {

    public Collector(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public MechanicProfile<Collector> getProfile() {
        return Profiles.COLLECTOR;
    }
}
