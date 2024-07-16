package dk.superawesome.factories.gui;

import dk.superawesome.factories.mehcanics.Mechanic;
import dk.superawesome.factories.util.Callback;
import dk.superawesome.factories.util.mappings.ItemMappings;
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
        getInventory().setItem(52, ItemMappings.get("writable_book").generateItem());
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
}
