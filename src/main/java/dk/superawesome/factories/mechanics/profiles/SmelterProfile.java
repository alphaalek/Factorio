package dk.superawesome.factories.mechanics.profiles;

import dk.superawesome.factories.building.Building;
import dk.superawesome.factories.building.Buildings;
import dk.superawesome.factories.gui.GuiFactory;
import dk.superawesome.factories.gui.impl.SmelterGui;
import dk.superawesome.factories.mechanics.MechanicFactory;
import dk.superawesome.factories.mechanics.MechanicProfile;
import dk.superawesome.factories.mechanics.impl.Smelter;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.concurrent.atomic.AtomicReference;

public class SmelterProfile implements MechanicProfile<Smelter, SmelterGui> {

    private final MechanicFactory<Smelter> factory = new SmelterMechanicFactory();
    private final GuiFactory<Smelter, SmelterGui> guiFactory = new SmelterGuiFactory();

    @Override
    public String getName() {
        return "Smelter";
    }

    @Override
    public Building getBuilding() {
        return Buildings.SMELTER;
    }

    @Override
    public MechanicFactory<Smelter> getFactory() {
        return factory;
    }

    @Override
    public GuiFactory<Smelter, SmelterGui> getGuiFactory() {
        return guiFactory;
    }

    @Override
    public int getID() {
        return 1;
    }

    private static class SmelterMechanicFactory implements MechanicFactory<Smelter> {

        @Override
        public Smelter create(Location loc, BlockFace rotation) {
            return new Smelter(loc, rotation);
        }
    }

    private static class SmelterGuiFactory implements GuiFactory<Smelter, SmelterGui> {

        @Override
        public SmelterGui create(Smelter mechanic, AtomicReference<SmelterGui> inUseReference) {
            return new SmelterGui(mechanic, inUseReference);
        }
    }
}