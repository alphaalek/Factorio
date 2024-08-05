package dk.superawesome.factorio.mechanics;

import org.bukkit.inventory.ItemStack;

public interface Storage {

    ItemStack getStored();

    void setStored(ItemStack stored);

    int getAmount();

    void setAmount(int amount);

    int getCapacity();
}
