package dk.superawesome.factories.mechanics.profiles;

import dk.superawesome.factories.building.Building;
import dk.superawesome.factories.gui.GuiFactory;
import dk.superawesome.factories.gui.impl.PowerCentralGui;
import dk.superawesome.factories.mechanics.MechanicFactory;
import dk.superawesome.factories.mechanics.MechanicProfile;
import dk.superawesome.factories.mechanics.impl.PowerCentral;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.concurrent.atomic.AtomicReference;

public class PowerCentralProfile implements MechanicProfile<PowerCentral, PowerCentralGui> {

    private final MechanicFactory<PowerCentral> factory = new PowerCentralMechanicFactory();
    private final GuiFactory<PowerCentral, PowerCentralGui> guiFactory = new PowerCentralGuiFactory();

    @Override
    public String getName() {
        return "Power Central";
    }

    @Override
    public Building getBuilding() {
        return null;
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
    public int getID() {
        return 3;
    }

    private static class PowerCentralMechanicFactory implements MechanicFactory<PowerCentral> {

        @Override
        public PowerCentral create(Location loc, BlockFace rotation) {
            return new PowerCentral(loc, rotation);
        }
    }

    private static class PowerCentralGuiFactory implements GuiFactory<PowerCentral, PowerCentralGui> {

        @Override
        public PowerCentralGui create(PowerCentral mechanic, AtomicReference<PowerCentralGui> inUseReference) {
            return new PowerCentralGui(mechanic, inUseReference);
        }
    }
}
