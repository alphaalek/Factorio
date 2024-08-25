package dk.superawesome.factorio.mechanics.profiles.power;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.GuiFactory;
import dk.superawesome.factorio.gui.impl.PowerCentralGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.power.PowerCentral;
import dk.superawesome.factorio.util.Array;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.concurrent.atomic.AtomicReference;

public class PowerCentralProfile implements GuiMechanicProfile<PowerCentral> {

    private final MechanicFactory<PowerCentral> factory = new PowerCentralMechanicFactory();
    private final GuiFactory<PowerCentral, PowerCentralGui> guiFactory = new PowerCentralGuiFactory();

    @Override
    public String getName() {
        return "Power Central";
    }

    @Override
    public Building getBuilding() {
        return Buildings.POWER_CENTRAL;
    }

    @Override
    public MechanicFactory<PowerCentral> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<PowerCentral> getStorageProvider() {
        return null;
    }

    @Override
    public GuiFactory<PowerCentral, PowerCentralGui> getGuiFactory() {
        return guiFactory;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return MechanicLevel.Registry.Builder.make(4)
                .mark(PowerCentral.CAPACITY, Array.fromData(1000d, 2500d, 3000d))
                .build();
    }

    @Override
    public int getID() {
        return 3;
    }

    private static class PowerCentralMechanicFactory implements MechanicFactory<PowerCentral> {

        @Override
        public PowerCentral create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new PowerCentral(loc, rotation, context);
        }
    }

    private static class PowerCentralGuiFactory implements GuiFactory<PowerCentral, PowerCentralGui> {

        @Override
        public PowerCentralGui create(PowerCentral mechanic, AtomicReference<PowerCentralGui> inUseReference) {
            return new PowerCentralGui(mechanic, inUseReference);
        }
    }
}
