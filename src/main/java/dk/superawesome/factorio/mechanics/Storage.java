package dk.superawesome.factorio.mechanics;

import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

public interface Storage {

    ItemStack getStored();

    void setStored(ItemStack stored);

    default Predicate<ItemStack> accepts() {
        // accepts all items by default if no item is stored
        return item -> getStored() == null || getStored().isSimilar(item);
    }

    int getAmount();

    void setAmount(int amount);

    int getCapacity();
}
