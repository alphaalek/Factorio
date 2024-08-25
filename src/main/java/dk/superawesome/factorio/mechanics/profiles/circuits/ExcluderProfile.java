package dk.superawesome.factorio.mechanics.profiles.circuits;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.circuits.Excluder;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class ExcluderProfile implements MechanicProfile<Excluder> {

    private static final MechanicFactory<Excluder> factory = new ExcluderMechanicFactory();

    @Override
    public String getName() {
        return "Excluder";
    }

    @Override
    public Building getBuilding() {
        return Buildings.GATE;
    }

    @Override
    public MechanicFactory<Excluder> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<Excluder> getStorageProvider() {
        return null;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return null;
    }

    @Override
    public int getID() {
        return 20;
    }

    private static class ExcluderMechanicFactory implements MechanicFactory<Excluder> {

        @Override
        public Excluder create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new Excluder(loc, rotation, context);
        }
    }
}
