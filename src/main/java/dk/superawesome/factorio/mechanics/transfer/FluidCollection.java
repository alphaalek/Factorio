package dk.superawesome.factorio.mechanics.transfer;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Predicate;

public interface FluidCollection extends TransferCollection {

    int CAPACITY_MARK = 0;

    boolean has(ItemStack stack);

    boolean has(Predicate<ItemStack> stack);

    List<ItemStack> take(int amount);
}
