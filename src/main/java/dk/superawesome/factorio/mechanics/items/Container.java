package dk.superawesome.factorio.mechanics.items;

import dk.superawesome.factorio.gui.BaseGui;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface Container {

    void pipePut(ItemCollection collection);

    int getCapacity();

    default <G extends BaseGui<G>> List<ItemStack> take(int amount, ItemStack stored, int storedAmount, AtomicReference<G> inUse, Consumer<G> doGui, Updater<Integer> updater) {
        List<ItemStack> items = new ArrayList<>();
        int taken = 0;
        while (taken < amount && taken < storedAmount) {
            ItemStack item = stored.clone();
            int a = Math.min(item.getMaxStackSize(), Math.min(storedAmount, amount) - taken);

            taken += a;
            item.setAmount(a);
            items.add(item);
        }

        updater.set(taken);
        G gui = inUse.get();
        if (gui != null) {
            doGui.accept(gui);
        }

        return items;
    }

    default <G extends BaseGui<G>> int put(ItemCollection from, int take, AtomicReference<G> inUse, BiConsumer<G, Integer> doGui, Updater<ItemStack> updater) {
        List<ItemStack> items = from.take(64);
        int add = 0;
        for (ItemStack item : items) {
            add += item.getAmount();

            if (updater.get() == null) {
                ItemStack type = item.clone();
                type.setAmount(1);
                updater.set(type);
            }
        }

        if (add > 0) {
            G gui = inUse.get();
            if (gui != null) {
                doGui.accept(gui, add);
            }
        }

        return add;
    }

    interface Updater<T> {

        T get();

        void set(T val);
    }
}