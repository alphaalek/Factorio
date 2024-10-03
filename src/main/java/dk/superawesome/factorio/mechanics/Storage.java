package dk.superawesome.factorio.mechanics;

import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

public interface Storage {

    ItemStack getStored();

    void setStored(ItemStack stored);

    default Predicate<ItemStack> getFilter() {
        return i -> true;
    }

    int getAmount();

    void setAmount(int amount);

    int getCapacity();

    default void ensureValidStorage() {
        if (getStored() != null && getAmount() > 0) {
            setStored(null);
        }
    }
}
