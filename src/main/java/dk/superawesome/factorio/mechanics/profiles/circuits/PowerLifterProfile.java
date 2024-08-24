package dk.superawesome.factorio.mechanics.profiles.circuits;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.circuits.PowerLifter;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class PowerLifterProfile implements MechanicProfile<PowerLifter> {

    private static final MechanicFactory<PowerLifter> factory = new PowerLifterMechanicFactory();

    @Override
    public String getName() {
        return "Power Lifter";
    }

    @Override
    public Building getBuilding() {
        return Buildings.POWER_LIFTER;
    }

    @Override
    public MechanicFactory<PowerLifter> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<PowerLifter> getStorageProvider() {
        return null;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return null;
    }

    @Override
    public int getID() {
        return 18;
    }

    private static class PowerLifterMechanicFactory implements MechanicFactory<PowerLifter> {

        @Override
        public PowerLifter create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new PowerLifter(loc, rotation, context);
        }
    }
}
