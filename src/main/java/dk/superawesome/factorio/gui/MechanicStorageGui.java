package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.Storage;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface MechanicStorageGui {

    int getInputContext();

    int getOutputContext();

    Inventory getInventory();

    MechanicGui<?, ?> getMechanicGui();

    Mechanic<?> getMechanic();

    Storage getStorage(int context);

    default int updateAddedItems(int amount, ItemStack item, List<Integer> slots) {
        return getMechanicGui().updateAddedItems(getInventory(), amount, item, slots);
    }

    default int updateAddedItems(Function<Integer, ItemStack> getStack, BiConsumer<Integer, ItemStack> setStack,
                                 int amount, ItemStack item, List<Integer> slots) {
        return getMechanicGui().updateAddedItems(getStack, setStack, amount, item, slots);
    }

    default int updateRemovedItems(int amount, ItemStack item, List<Integer> slots) {
        return getMechanicGui().updateRemovedItems(getInventory(), amount, item, slots);
    }

    default int updateRemovedItems(Function<Integer, ItemStack> getStack, int amount, ItemStack item, List<Integer> slots) {
        return getMechanicGui().updateRemovedItems(getStack, amount, item, slots);
    }

    default void updateAmount(int context, ItemStack added, Inventory source, int diff, List<Integer> slots) {
        getMechanicGui().updateAmount(getMechanic().getProfile().getStorageProvider().createStorage(getMechanic(), context), source, diff, i -> updateRemovedItems(i, added, slots));
    }

    default int calculateAmount(List<Integer> slots) {
        return getMechanicGui().calculateAmount(slots);
    }
}
