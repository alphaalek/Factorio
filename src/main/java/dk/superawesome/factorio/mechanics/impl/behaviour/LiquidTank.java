package dk.superawesome.factorio.mechanics.impl.behaviour;

import dk.superawesome.factorio.mechanics.AbstractMechanic;
import dk.superawesome.factorio.mechanics.MechanicProfile;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.mechanics.Profiles;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class LiquidTank extends AbstractMechanic<LiquidTank> {

    public LiquidTank(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public MechanicProfile<LiquidTank> getProfile() {
        return Profiles.LIQUID_TANK;
    }
}
