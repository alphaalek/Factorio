package dk.superawesome.factories.mechanics.profiles;

import dk.superawesome.factories.building.Building;
import dk.superawesome.factories.building.Buildings;
import dk.superawesome.factories.gui.GuiFactory;
import dk.superawesome.factories.gui.impl.ConstructorGui;
import dk.superawesome.factories.mechanics.MechanicFactory;
import dk.superawesome.factories.mechanics.MechanicProfile;
import dk.superawesome.factories.mechanics.impl.Constructor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.concurrent.atomic.AtomicReference;

public class ConstructorProfile implements MechanicProfile<Constructor, ConstructorGui> {

    private final MechanicFactory<Constructor> factory = new ConstructorMechanicFactory();
    private final GuiFactory<Constructor, ConstructorGui> guiFactory = new ConstructorGuiFactory();

    @Override
    public String getName() {
        return "Constructor";
    }

    @Override
    public Building getBuilding() {
        return Buildings.CONSTRUCTOR;
    }

    @Override
    public MechanicFactory<Constructor> getFactory() {
        return factory;
    }

    @Override
    public GuiFactory<Constructor, ConstructorGui> getGuiFactory() {
        return guiFactory;
    }

    @Override
    public int getID() {
        return 0;
    }

    private static class ConstructorMechanicFactory implements MechanicFactory<Constructor> {

        @Override
        public Constructor create(Location loc, BlockFace rotation) {
            return new Constructor(loc, rotation);
        }
    }

    private static class ConstructorGuiFactory implements GuiFactory<Constructor, ConstructorGui> {

        @Override
        public ConstructorGui create(Constructor constructor, AtomicReference<ConstructorGui> inUseReference) {
            return new ConstructorGui(constructor, inUseReference);
        }
    }
}
