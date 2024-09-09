package dk.superawesome.factorio.mechanics.profiles.relative;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.relative.Comparator;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class ComparatorProfile implements MechanicProfile<Comparator> {

    private static final MechanicFactory<Comparator> factory = new ComparatorMechanicFactory();

    @Override
    public String getName() {
        return "Comparator";
    }

    @Override
    public Building getBuilding(Mechanic<?> forMechanic) {
        return Buildings.COMPARATOR;
    }

    @Override
    public MechanicFactory<Comparator> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<Comparator> getStorageProvider() {
        return null;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return null;
    }

    @Override
    public int getID() {
        return 12;
    }

    private static class ComparatorMechanicFactory implements MechanicFactory<Comparator> {

        @Override
        public Comparator create(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign) {
            return new Comparator(loc, rotation, context, hasWallSign);
        }
    }
}
