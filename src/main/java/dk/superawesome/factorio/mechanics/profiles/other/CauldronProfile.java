package dk.superawesome.factorio.mechanics.profiles.other;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.other.Cauldron;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class CauldronProfile implements MechanicProfile<Cauldron> {

    private static final MechanicFactory<Cauldron> factory = new CauldronMechanicFactory();

    @Override
    public String getName() {
        return "Cauldron";
    }

    @Override
    public Building getBuilding(boolean hasWallSign) {
        return Buildings.CAULDRON;
    }

    @Override
    public MechanicFactory<Cauldron> getFactory() {
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
        return 14;
    }

    private static class CauldronMechanicFactory implements MechanicFactory<Cauldron> {

        @Override
        public Cauldron create(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign) {
            return new Cauldron(loc, rotation, context, hasWallSign);
        }
    }

    @Override
    public boolean isInteractable() {
        return true;
    }
}
