package dk.superawesome.factorio.mechanics.profiles.circuits;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.circuits.Cauldron;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class CauldronProfile implements MechanicProfile<Cauldron> {

    private static final MechanicFactory<Cauldron> factory = new CauldronMechanicFactory();

    @Override
    public String getName() {
        return "Cauldron";
    }

    @Override
    public Building getBuilding() {
        return Buildings.CAULDRON;
    }

    @Override
    public MechanicFactory<Cauldron> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<Cauldron> getStorageProvider() {
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
        public Cauldron create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new Cauldron(loc, rotation, context);
        }
    }

    @Override
    public boolean isInteractable() {
        return true;
    }
}
