package dk.superawesome.factorio.mechanics.profiles.relative;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.relative.Hopper;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class HopperProfile implements MechanicProfile<Hopper> {

    private static final MechanicFactory<Hopper> factory = new HopperMechanicFactory();

    @Override
    public String getName() {
        return "Hopper";
    }

    @Override
    public Building getBuilding(boolean hasWallSign) {
        return Buildings.COLLECTOR;
    }

    @Override
    public MechanicFactory<Hopper> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider getStorageProvider() {
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
        public Hopper create(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign) {
            return new Hopper(loc, rotation, context, hasWallSign);
        }
    }
}
