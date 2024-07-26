package dk.superawesome.factories.mechanics.profiles;

import dk.superawesome.factories.building.Building;
import dk.superawesome.factories.building.Buildings;
import dk.superawesome.factories.gui.GuiFactory;
import dk.superawesome.factories.gui.impl.StorageBoxGui;
import dk.superawesome.factories.mechanics.*;
import dk.superawesome.factories.mechanics.impl.StorageBox;
import dk.superawesome.factories.mechanics.items.ItemCollection;
import dk.superawesome.factories.util.Array;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

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
    public MechanicLevel.Registry getLevelRegistry() {
        return MechanicLevel.Registry.Builder
                .make(5)
                .mark(ItemCollection.CAPACITY_MARK, Array.fromData(64 * 3, 64 * 125, 64 * 175, 64 * 250, 64 * 500))
                .build();
    }

    @Override
    public int getID() {
        return 2;
    }

    private static class StorageBoxMechanicFactory implements MechanicFactory<StorageBox> {

        @Override
        public StorageBox create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new StorageBox(loc, rotation, context);
        }
    }

    private static class StorageBoxGuiFactory implements GuiFactory<StorageBox, StorageBoxGui> {

        @Override
        public StorageBoxGui create(StorageBox mechanic, AtomicReference<StorageBoxGui> inUseReference) {
            return new StorageBoxGui(mechanic, inUseReference);
        }
    }
}
