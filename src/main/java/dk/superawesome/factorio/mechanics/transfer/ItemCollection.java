package dk.superawesome.factorio.mechanics.transfer;

import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.mechanics.Storage;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface ItemCollection extends TransferCollection {

    int CAPACITY_MARK = 0;

    boolean has(ItemStack stack);

    boolean has(Predicate<ItemStack> stack);

    List<ItemStack> take(int amount);

    default <G extends BaseGui<G>> List<ItemStack> take(int amount, ItemStack stored, int storedAmount, AtomicReference<G> inUse, BiConsumer<G, Integer> doGui, Storage storage) {
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
        storage.setAmount(storage.getAmount() - taken);

        return items;
    }
}
