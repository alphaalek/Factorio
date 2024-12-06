package dk.superawesome.factorio.mechanics.profiles.power;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.power.SolarCell;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class SolarCellProfile implements MechanicProfile<SolarCell> {

    private static final MechanicFactory<SolarCell> factory = new SolarCellMechanicFactory();

    @Override
    public String getName() {
        return "Solar Cell";
    }

    @Override
    public Building getBuilding(boolean hasWallSign) {
        return Buildings.SOLAR_CELL;
    }

    @Override
    public MechanicFactory<SolarCell> getFactory() {
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
        return 19;
    }

    private static class SolarCellMechanicFactory implements MechanicFactory<SolarCell> {

        @Override
        public SolarCell create(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
            return new SolarCell(loc, rotation, context, hasWallSign, isBuild);
        }
    }
}
