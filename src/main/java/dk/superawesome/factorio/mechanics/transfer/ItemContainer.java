package dk.superawesome.factorio.mechanics.transfer;

import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.gui.MechanicStorageGui;
import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

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

    default <G extends BaseGui<G>> List<ItemStack> take(int amount, ItemStack stored, int storedAmount, AtomicReference<G> inUse, BiConsumer<G, Integer> doGui, HeapToStackAccess<Integer> updater) {
        List<ItemStack> items = new ArrayList<>();
        int taken = 0;
        while (taken < amount && taken < storedAmount) {
            ItemStack item = stored.clone();
            int a = Math.min(item.getMaxStackSize(), Math.min(storedAmount, amount) - taken);

            taken += a;
            item.setAmount(a);
            items.add(item);
        }

        G gui = inUse.get();
        if (gui != null) {
            doGui.accept(gui, taken);
        }
        updater.set(taken);

        return items;
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
