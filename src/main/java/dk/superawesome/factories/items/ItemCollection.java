package dk.superawesome.factories.items;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Predicate;

public interface ItemCollection {

    boolean has(ItemStack stack);

    boolean has(Predicate<ItemStack> stack);

    List<ItemStack> take(int amount);
}
