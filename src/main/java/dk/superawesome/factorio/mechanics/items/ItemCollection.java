package dk.superawesome.factorio.mechanics.items;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Predicate;

public interface ItemCollection {

    int CAPACITY_MARK = 0;

    boolean has(ItemStack stack);

    boolean has(Predicate<ItemStack> stack);

    List<ItemStack> take(int amount);

    boolean isEmpty();

    double getEnergyCost();
}
