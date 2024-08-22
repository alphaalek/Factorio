package dk.superawesome.factorio.mechanics.profiles.circuits;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.circuits.Station;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class StationProfile implements MechanicProfile<Station> {

    private static final MechanicFactory<Station> factory = new OneWayMechanicFactory();

    @Override
    public String getName() {
        return "Station";
    }

    @Override
    public Building getBuilding() {
        return Buildings.GATE;
    }

    @Override
    public MechanicFactory<Station> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<Station> getStorageProvider() {
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

    private static class OneWayMechanicFactory implements MechanicFactory<Station> {

        @Override
        public Station create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new Station(loc, rotation, context);
        }
    }
}
