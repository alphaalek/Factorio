package dk.superawesome.factorio.mechanics.profiles.power;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.power.PowerExpander;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class PowerExpanderProfile implements MechanicProfile<PowerExpander> {

    private final MechanicFactory<PowerExpander> factory = new PowerExpanderMechanicFactory();
    @Override
    public String getName() {
        return "Power Expander";
    }

    @Override
    public Building getBuilding(boolean hasWallSign) {
        return Buildings.GATE.get(hasWallSign);
    }

    @Override
    public MechanicFactory<PowerExpander> getFactory() {
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
        return 10;
    }

    private static class PowerExpanderMechanicFactory implements MechanicFactory<PowerExpander> {

        @Override
        public PowerExpander create(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
            return new PowerExpander(loc, rotation, context, hasWallSign, isBuild);
        }
    }
}
