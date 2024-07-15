package dk.superawesome.factories.items;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface ItemCollection {

    boolean has(ItemStack stack);

    List<ItemStack> take(int amount);
}
