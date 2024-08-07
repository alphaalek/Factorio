package dk.superawesome.factorio.mechanics.profiles;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.Splitter;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class SplitterProfile implements MechanicProfile<Splitter> {

    private static final MechanicFactory<Splitter> factory = new SplitterMechanicFactory();

    @Override
    public String getName() {
        return "Splitter";
    }

    @Override
    public Building getBuilding() {
        return Buildings.GATE;
    }

    @Override
    public MechanicFactory<Splitter> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<Splitter> getStorageProvider() {
        return null;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return null;
    }

    @Override
    public int getID() {
        return 10;
    }

    private static class SplitterMechanicFactory implements MechanicFactory<Splitter> {

        @Override
        public Splitter create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new Splitter(loc, rotation, context);
        }
    }
}
