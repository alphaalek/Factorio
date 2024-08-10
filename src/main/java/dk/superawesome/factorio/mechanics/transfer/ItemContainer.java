package dk.superawesome.factorio.mechanics.transfer;

import dk.superawesome.factorio.gui.MechanicStorageGui;
import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.inventory.Inventory;

public interface ItemContainer extends Container<ItemCollection> {

    default boolean accepts(TransferCollection collection) {
        return collection instanceof ItemCollection;
    }

    default Inventory createVirtualOneWayInventory(Mechanic<?> mechanic, MechanicStorageGui gui, boolean inputAsIteratorSlots) {
        return new VirtualOneWayContainer(mechanic, gui,
                inputAsIteratorSlots
                        ? mechanic.getProfile().getStorageProvider().getSlots(gui.getInputContext())
                        : mechanic.getProfile().getStorageProvider().getSlots(gui.getOutputContext())
        );
    }
}
