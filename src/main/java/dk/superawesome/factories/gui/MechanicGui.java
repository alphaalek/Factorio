package dk.superawesome.factories.gui;

import dk.superawesome.factories.mehcanics.Mechanic;
import dk.superawesome.factories.util.Callback;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public abstract class MechanicGui<G extends BaseGui<G>, M extends Mechanic<M, G>> extends BaseGui<G> {

    public static class InitCallbackHolder implements Supplier<Callback> {

        private final Callback initCallback = new Callback();

        @Override
        public Callback get() {
            return initCallback;
        }
    }

    private final M mechanic;

    public MechanicGui(M mechanic, AtomicReference<G> inUseReference, Supplier<Callback> initCallback) {
        super(initCallback, inUseReference, BaseGui.DOUBLE_CHEST, mechanic.getProfile().getName() + " (Lvl " + mechanic.getLevel() + ")");
        this.mechanic = mechanic;
    }

    @Override
    public void loadItems() {
        getInventory().setItem(52, new ItemStack(Material.WRITABLE_BOOK));
    }

    public M getMechanic() {
        return mechanic;
    }

    protected void updateAddedItems(int amount, ItemStack stored, List<Integer> slots) {
        int left = amount;
        for (int i : slots) {
            ItemStack item = getInventory().getItem(i);
            if (item != null) {
                int add = Math.min(left, item.getMaxStackSize() - item.getAmount());

                left -= add;
                item.setAmount(item.getAmount() + add);

                if (left == 0) {
                    break;
                }
            }
        }

        if (left > 0) {
            for (int i : slots) {
                ItemStack item = getInventory().getItem(i);
                if (item == null) {
                    ItemStack added = stored.clone();
                    added.setAmount(left);
                    getInventory().setItem(i, added);
                    break;
                }
            }
        }
    }

    protected void updateRemovedItems(int amount, List<Integer> slots) {
        int left = amount;
        for (int i : slots) {
            ItemStack item = getInventory().getItem(i);
            if (item != null) {
                int remove = Math.min(left, item.getAmount());

                left -= remove;
                item.setAmount(item.getAmount() - remove);

                if (left == 0) {
                    break;
                }
            }
        }
    }

    protected void addItemsToSlots(ItemStack item, List<Integer> slots) {
        // find all the crafting grid items where the item can be added to

        int left = item.getAmount();
        int i = 0;
        while (left > 0 && i < slots.size()) {
            int slot = slots.get(i++);
            ItemStack crafting = getInventory().getItem(slot);

            if (crafting != null
                    && crafting.isSimilar(item)
                    && crafting.getAmount() < crafting.getMaxStackSize()) {
                int add = Math.min(left, crafting.getMaxStackSize() - crafting.getAmount());

                left -= add;
                crafting.setAmount(crafting.getAmount() + add);
            }
        }

        item.setAmount(left);

        // we still have some items left, iterate over the crafting grid slots again
        // and check if any of them are empty
        if (left > 0) {
            i = 0;
            while (i < slots.size()) {
                int slot = slots.get(i++);
                ItemStack crafting = getInventory().getItem(slot);

                if (crafting == null) {
                    getInventory().setItem(slot, item.clone());
                    item.setAmount(0);
                    break;
                }
            }
        }
    }

    protected void loadStorageTypes(ItemStack stored, int amount, List<Integer> slots) {
        int left = amount;
        int i = 0;
        while (left > 0 && i < slots.size()) {
            ItemStack item = stored.clone();
            int add = Math.min(item.getMaxStackSize(), left);

            item.setAmount(add);
            left -= add;

            getInventory().setItem(slots.get(i++), item);
        }
    }
}
