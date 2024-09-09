package dk.superawesome.factorio.mechanics.profiles.relative;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.relative.Collector;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class CollectorProfile implements MechanicProfile<Collector> {

    private static final MechanicFactory<Collector> factory = new CollectorMechanicFactory();

    @Override
    public String getName() {
        return "Collector";
    }

    @Override
    public Building getBuilding(Mechanic<?> forMechanic) {
        return Buildings.COLLECTOR;
    }

    @Override
    public MechanicFactory<Collector> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<Collector> getStorageProvider() {
        return null;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return null;
    }

    @Override
    public int getID() {
        return 7;
    }

    private static class CollectorMechanicFactory implements MechanicFactory<Collector> {

        @Override
        public Collector create(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign) {
            return new Collector(loc, rotation, context, hasWallSign);
        }
    }
}
