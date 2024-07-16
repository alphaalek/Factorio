package dk.superawesome.factories.mehcanics.profiles;

import dk.superawesome.factories.building.Building;
import dk.superawesome.factories.building.Buildings;
import dk.superawesome.factories.gui.BaseGui;
import dk.superawesome.factories.gui.GuiFactory;
import dk.superawesome.factories.gui.impl.StorageBoxGui;
import dk.superawesome.factories.mehcanics.MechanicFactory;
import dk.superawesome.factories.mehcanics.MechanicProfile;
import dk.superawesome.factories.mehcanics.impl.StorageBox;
import org.bukkit.Location;

import java.util.concurrent.atomic.AtomicReference;

public class StorageBoxProfile implements MechanicProfile<StorageBox, StorageBoxGui> {

    private final MechanicFactory<StorageBox> factory = new StorageBoxMechanicFactory();
    private final GuiFactory<StorageBox, StorageBoxGui> guiFactory = new StorageBoxGuiFactory();

    @Override
    public String getName() {
        return "Storage Box";
    }

    @Override
    public Building getBuilding() {
        return Buildings.STORAGE_BOX;
    }

    @Override
    public MechanicFactory<StorageBox> getFactory() {
        return factory;
    }

    @Override
    public GuiFactory<StorageBox, StorageBoxGui> getGuiFactory() {
        return guiFactory;
    }

    @Override
    public int getID() {
        return 1;
    }

    private static class StorageBoxMechanicFactory implements MechanicFactory<StorageBox> {

        @Override
        public StorageBox create(Location loc) {
            return new StorageBox(loc);
        }
    }

    private static class StorageBoxGuiFactory implements GuiFactory<StorageBox, StorageBoxGui> {

        @Override
        public StorageBoxGui create(StorageBox mechanic, AtomicReference<StorageBoxGui> inUseReference) {
            return new StorageBoxGui(mechanic, inUseReference);
        }
    }
}
