package dk.superawesome.factorio.mechanics.transfer;

import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.gui.MechanicStorageGui;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.MechanicLevel;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public interface ItemContainer extends Container<ItemCollection> {

    default boolean accepts(TransferCollection collection) {
        return collection instanceof ItemCollection && collection != this;
    }

    default Inventory createVirtualOneWayInventory(Mechanic<?> mechanic, Container<?> container, MechanicStorageGui gui, boolean inputAsIteratorSlots) {
        return new VirtualOneWayContainer(mechanic, container, gui,
                inputAsIteratorSlots
                        ? mechanic.getProfile().getStorageProvider().getSlots(gui.getInputContext())
                        : mechanic.getProfile().getStorageProvider().getSlots(gui.getOutputContext())
        );
    }

    default <G extends BaseGui<G>> int put(ItemCollection from, int take, AtomicReference<G> inUse, BiConsumer<G, Integer> doGui, HeapToStackAccess<ItemStack> access) {
        List<ItemStack> items = from.take(take);
        int add = 0;
        for (ItemStack item : items) {
            add += item.getAmount();

            if (access.get() == null) {
                ItemStack type = item.clone();
                type.setAmount(1);
                access.set(type);
            }
        }

        if (add > 0) {
            G gui = inUse.get();
            if (gui != null) {
                doGui.accept(gui, add);
            }
        }

        return add;
    }
}
