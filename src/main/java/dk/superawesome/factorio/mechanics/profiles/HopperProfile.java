package dk.superawesome.factorio.mechanics.profiles;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.circuits.Hopper;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class HopperProfile implements MechanicProfile<Hopper> {

    private static final MechanicFactory<Hopper> factory = new HopperMechanicFactory();

    @Override
    public String getName() {
        return "Hopper";
    }

    @Override
    public Building getBuilding() {
        return Buildings.COLLECTOR;
    }

    @Override
    public MechanicFactory<Hopper> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<Hopper> getStorageProvider() {
        return null;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return null;
    }

    @Override
    public int getID() {
        return 8;
    }

    private static class HopperMechanicFactory implements MechanicFactory<Hopper> {

        @Override
        public Hopper create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new Hopper(loc, rotation, context);
        }
    }
}
