package dk.superawesome.factorio.mechanics.impl;

import dk.superawesome.factorio.gui.impl.GeneratorGui;
import dk.superawesome.factorio.mechanics.AbstractMechanic;
import dk.superawesome.factorio.mechanics.MechanicProfile;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.mechanics.Profiles;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class Generator extends AbstractMechanic<Generator, GeneratorGui> {

    public Generator(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public MechanicProfile<Generator, GeneratorGui> getProfile() {
        return Profiles.GENERATOR;
    }
}
