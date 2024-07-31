package dk.superawesome.factorio.mechanics.impl;

import dk.superawesome.factorio.gui.impl.AssemblerGui;
import dk.superawesome.factorio.mechanics.AbstractMechanic;
import dk.superawesome.factorio.mechanics.MechanicProfile;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.mechanics.Profiles;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class Assembler extends AbstractMechanic<Assembler, AssemblerGui> {

    public Assembler(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public MechanicProfile<Assembler, AssemblerGui> getProfile() {
        return Profiles.ASSEMBLER;
    }
}
