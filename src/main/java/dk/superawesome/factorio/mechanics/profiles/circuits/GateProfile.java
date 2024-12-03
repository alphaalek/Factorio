package dk.superawesome.factorio.mechanics.profiles.circuits;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.circuits.Gate;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class GateProfile implements MechanicProfile<Gate> {

    private static final MechanicFactory<Gate> factory = new GateMechanicFactory();

    @Override
    public String getName() {
        return "Gate";
    }

    @Override
    public Building getBuilding(boolean hasWallSign) {
        return Buildings.GATE.get(hasWallSign);
    }

    @Override
    public MechanicFactory<Gate> getFactory() {
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
        return 9;
    }

    private static class GateMechanicFactory implements MechanicFactory<Gate> {

        @Override
        public Gate create(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign) {
            return new Gate(loc, rotation, context, hasWallSign);
        }
    }
}
