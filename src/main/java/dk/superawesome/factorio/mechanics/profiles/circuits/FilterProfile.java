package dk.superawesome.factorio.mechanics.profiles.circuits;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.circuits.Filter;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class FilterProfile implements MechanicProfile<Filter> {

    private static final MechanicFactory<Filter> factory = new FilterMechanicFactory();

    @Override
    public String getName() {
        return "Filter";
    }

    @Override
    public Building getBuilding(Mechanic<?> forMechanic) {
        return Buildings.GATE.get(forMechanic);
    }

    @Override
    public MechanicFactory<Filter> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<Filter> getStorageProvider() {
        return null;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return null;
    }

    @Override
    public boolean isInteractable() {
        return true;
    }

    @Override
    public int getID() {
        return 11;
    }

    private static class FilterMechanicFactory implements MechanicFactory<Filter> {

        @Override
        public Filter create(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign) {
            return new Filter(loc, rotation, context, hasWallSign);
        }
    }
}
