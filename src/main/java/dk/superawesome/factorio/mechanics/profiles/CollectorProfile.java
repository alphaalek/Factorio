package dk.superawesome.factorio.mechanics.profiles;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.MechanicFactory;
import dk.superawesome.factorio.mechanics.MechanicLevel;
import dk.superawesome.factorio.mechanics.MechanicProfile;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.mechanics.impl.Assembler;
import dk.superawesome.factorio.mechanics.impl.Collector;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class CollectorProfile implements MechanicProfile<Collector> {

    private static final MechanicFactory<Collector> factory = new CollectorMechanicFactory();

    @Override
    public String getName() {
        return "Collector";
    }

    @Override
    public Building getBuilding() {
        return Buildings.COLLECTOR;
    }

    @Override
    public MechanicFactory<Collector> getFactory() {
        return factory;
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
        public Collector create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new Collector(loc, rotation, context);
        }
    }
}
