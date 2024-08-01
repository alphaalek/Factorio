package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.util.Callback;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class MechanicGui<G extends BaseGui<G>, M extends Mechanic<M, G>> extends BaseGui<G> {

    private final M mechanic;

    public MechanicGui(M mechanic, AtomicReference<G> inUseReference, Supplier<Callback> initCallback, String title) {
        super(initCallback, inUseReference, BaseGui.DOUBLE_CHEST, title);
        this.mechanic = mechanic;
    }

    public MechanicGui(M mechanic, AtomicReference<G> inUseReference, Supplier<Callback> initCallback) {
        this(mechanic, inUseReference, initCallback, mechanic.getProfile().getName() + " (Lvl " + mechanic.getLevel() + ")");
    }

    @Override
    public void loadItems() {
        int i = 0;
        for (GuiElement element : getGuiElements()) {
            int slot = 51 + i++;
            registerEvent(slot, event -> element.handle(event, (Player) event.getWhoClicked(), this));
            getInventory().setItem(slot, element.getItem());
        }

        loadInputOutputItems();
    }

    public abstract void loadInputOutputItems();

    protected List<GuiElement> getGuiElements() {
        return Arrays.asList(Elements.UPGRADE, Elements.MEMBERS, Elements.DELETE);
    }

    public M getMechanic() {
        return mechanic;
    }

    protected int updateAddedItems(Inventory inventory, int amount, ItemStack stored, List<Integer> slots) {
        // find all the slots with items similar to the stored item and add to these slots
        int left = amount;
        for (int i : slots) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.isSimilar(stored)) {
                int add = Math.min(left, item.getMaxStackSize() - item.getAmount());

                left -= add;
                item.setAmount(item.getAmount() + add);

                if (left == 0) {
                    break;
                }
            }
        }

        // we still have some items left to be added, find the empty slots and add to them
        if (left > 0) {
            for (int i : slots) {
                ItemStack item = inventory.getItem(i);
                if (item == null) {
                    int add = Math.min(left, stored.getMaxStackSize());

                    left -= add;
                    ItemStack added = stored.clone();
                    added.setAmount(add);

                    inventory.setItem(i, added);
                }

                if (left == 0) {
                    break;
                }
            }
        }

        return left;
    }

    protected int updateRemovedItems(Inventory inventory, int amount, ItemStack stored, List<Integer> slots) {
        int left = amount;
        for (int i : slots) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.isSimilar(stored)) {
                int remove = Math.min(left, item.getAmount());

                left -= remove;
                item.setAmount(item.getAmount() - remove);

                if (left == 0) {
                    break;
                }
            }
        }

        return left;
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

    protected List<Integer> reverseSlots(List<Integer> slots) {
        return IntStream.range(0, slots.size())
                .boxed()
                .map(slots::get)
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
    }
}
