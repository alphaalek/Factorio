package dk.superawesome.factorio.mechanics.profiles.circuits;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.circuits.AssemblerTrigger;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class AssemblerTriggerProfile implements MechanicProfile<AssemblerTrigger> {

    private static final MechanicFactory<AssemblerTrigger> factory = new AssemblerTriggerMechanicFactory();

    @Override
    public String getName() {
        return "Assembler Trigger";
    }

    @Override
    public String getSignName() {
        return "Assemb. Trigger";
    }

    @Override
    public Building getBuilding() {
        return Buildings.ASSEMBLER_TRIGGER;
    }

    @Override
    public MechanicFactory<AssemblerTrigger> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<AssemblerTrigger> getStorageProvider() {
        return null;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return null;
    }

    @Override
    public int getID() {
        return 15;
    }

    @Override
    public boolean isInteractable() {
        return true;
    }

    private static class AssemblerTriggerMechanicFactory implements MechanicFactory<AssemblerTrigger> {

        @Override
        public AssemblerTrigger create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new AssemblerTrigger(loc, rotation, context);
        }
    }
}