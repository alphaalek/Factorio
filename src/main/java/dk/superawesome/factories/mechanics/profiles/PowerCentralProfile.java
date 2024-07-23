package dk.superawesome.factories.mechanics.profiles;

import dk.superawesome.factories.building.Building;
import dk.superawesome.factories.building.Buildings;
import dk.superawesome.factories.gui.GuiFactory;
import dk.superawesome.factories.gui.impl.PowerCentralGui;
import dk.superawesome.factories.mechanics.MechanicFactory;
import dk.superawesome.factories.mechanics.MechanicLevel;
import dk.superawesome.factories.mechanics.MechanicProfile;
import dk.superawesome.factories.mechanics.MechanicStorageContext;
import dk.superawesome.factories.mechanics.impl.PowerCentral;
import dk.superawesome.factories.util.Array;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.concurrent.atomic.AtomicReference;

public class PowerCentralProfile implements MechanicProfile<PowerCentral, PowerCentralGui> {

    public static final int CAPACITY = 0;

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
    public GuiFactory<PowerCentral, PowerCentralGui> getGuiFactory() {
        return guiFactory;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return MechanicLevel.Registry.Builder.make(4)
                .mark(CAPACITY, Array.fromData(1000d, 2500d, 3000d))
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
