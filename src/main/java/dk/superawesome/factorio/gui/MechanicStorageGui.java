package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.Storage;
import dk.superawesome.factorio.mechanics.StorageProvider;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

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

    default int updateRemovedItems(int amount, ItemStack item, List<Integer> slots) {
        return getMechanicGui().updateRemovedItems(getInventory(), amount, item, slots);
    }

    default void updateAmount(int context, ItemStack added, Inventory source, int diff, List<Integer> slots) {
        getMechanicGui().updateAmount(getMechanic().getProfile().getStorageProvider().createStorage(getMechanic(), context), source, diff, i -> updateRemovedItems(i, added, slots));
    }

    default int calculateAmount(List<Integer> slots) {
        return getMechanicGui().calculateAmount(slots);
    }
}
