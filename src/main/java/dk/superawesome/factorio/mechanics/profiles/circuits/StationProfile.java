package dk.superawesome.factorio.mechanics.profiles.circuits;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.circuits.Station;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class StationProfile implements MechanicProfile<Station> {

    private static final MechanicFactory<Station> factory = new StationMechanicFactory();

    @Override
    public String getName() {
        return "Station";
    }

    @Override
    public Building getBuilding(boolean hasWallSign) {
        return Buildings.GATE.get(hasWallSign);
    }

    @Override
    public MechanicFactory<Station> getFactory() {
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
        return 17;
    }

    private static class StationMechanicFactory implements MechanicFactory<Station> {

        @Override
        public Station create(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign) {
            return new Station(loc, rotation, context, hasWallSign);
        }
    }
}
