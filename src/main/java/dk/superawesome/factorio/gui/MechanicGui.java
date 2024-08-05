package dk.superawesome.factorio.gui;

import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIAction;
import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.FuelMechanic;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.Storage;
import dk.superawesome.factorio.mechanics.StorageProvider;
import dk.superawesome.factorio.util.Callback;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class MechanicGui<G extends BaseGui<G>, M extends Mechanic<M>> extends BaseGuiAdapter<G> {

    private final M mechanic;

    public MechanicGui(M mechanic, AtomicReference<G> inUseReference, Supplier<Callback> initCallback, String title) {
        super(initCallback, inUseReference, BaseGui.DOUBLE_CHEST, title, true);
        this.mechanic = mechanic;
    }

    public MechanicGui(M mechanic, AtomicReference<G> inUseReference, Supplier<Callback> initCallback) {
        this(mechanic, inUseReference, initCallback, mechanic.toString());
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

    protected void openSignGuiAndCall(Player p, String total, Consumer<Double> function) {
        SignGUI gui = SignGUI.builder()
                .setLines("", "/ " + total, "---------------", "Vælg antal")
                // ensure synchronous call
                .callHandlerSynchronously(Factorio.get())
                // calls when the gui is closed
                .setHandler((player, result) -> {
                    // get the amount chosen and apply this to the function
                    double amount = 0;
                    try {
                        amount = Double.parseDouble(result.getLine(0));
                    } catch (NumberFormatException ex) {
                        // ignore
                    }

                    // ony apply the function if the player wrote a valid amount
                    if (amount <= 0) {
                        player.sendMessage("§cUgyldigt antal valgt!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                    } else {
                        function.accept(amount);
                    }

                    // return to the gui and reload view
                    return Arrays.asList(
                            SignGUIAction.openInventory(Factorio.get(), getInventory()), SignGUIAction.runSync(Factorio.get(), this::loadInputOutputItems));
                })
                .build();
        gui.open(p);
    }

    protected int getAffectedAmountForAction(InventoryAction action, ItemStack item) {
        return switch (action) {
            case DROP_ALL_CURSOR, DROP_ALL_SLOT, PICKUP_ALL, PICKUP_SOME, MOVE_TO_OTHER_INVENTORY -> item.getAmount();
            case DROP_ONE_SLOT, DROP_ONE_CURSOR, PICKUP_ONE -> 1;
            case PICKUP_HALF -> (int) Math.round(item.getAmount() / 2d);
            default -> 0;
        };
    }

    public void updateFuelState(List<Integer> slots) {
        if (getMechanic() instanceof FuelMechanic fuelMechanic) {
            int blaze = Math.round(fuelMechanic.getCurrentFuelAmount() * slots.size());
            int times = 0;
            for (int slot : slots) {
                if (++times > blaze) {
                    getInventory().setItem(slot, new ItemStack(Material.RED_STAINED_GLASS_PANE));
                    continue;
                }

                getInventory().setItem(slot, new ItemStack(Material.BLAZE_POWDER));
            }
        }
    }

    protected List<ItemStack> findItems(List<Integer> slots) {
        List<ItemStack> items = new ArrayList<>();
        for (int i : slots) {
            ItemStack item = getInventory().getItem(i);
            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    protected Storage getStorage(int context) {
        StorageProvider<M> provider = mechanic.getProfile().getStorageProvider();
        if (provider != null) {
            return provider.createStorage(mechanic, context);
        }

        return null;
    }

    protected boolean handleOnlyCollectInteraction(InventoryClickEvent event, Storage storage) {
        if (event.getAction() == InventoryAction.PLACE_ONE
                || event.getAction() == InventoryAction.PLACE_ALL
                || event.getAction() == InventoryAction.PLACE_SOME
                || event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
            return true;
        }

        if (event.getCurrentItem() != null) {
            storage.setAmount(storage.getAmount() - getAffectedAmountForAction(event.getAction(), event.getCurrentItem()));
        }

        return false;
    }

    protected boolean handleOnlyHotbarCollectInteraction(InventoryClickEvent event, Storage storage) {
        ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
        ItemStack at  = event.getView().getItem(event.getRawSlot());

        if (at != null && hotbarItem != null) {
            if (hotbarItem.isSimilar(at)) {
                int add = Math.min(at.getAmount(), hotbarItem.getMaxStackSize() - hotbarItem.getAmount());
                hotbarItem.setAmount(hotbarItem.getAmount() + add);
                at.setAmount(at.getAmount() - add);

                storage.setAmount(storage.getAmount() - add);
            }

            return true;
        } else if (at != null) {
            storage.setAmount(storage.getAmount() - at.getAmount());
        }

        // don't allow inserting items
        if (at == null) {
            return true;
        }

        return false;
    }

    protected void updateAmount(Storage storage, HumanEntity adder, List<Integer> slots, Consumer<Integer> leftover) {
        getMechanic().getTickThrottle().throttle();

        int before = findItems(slots).stream().mapToInt(ItemStack::getAmount).sum();
        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
            // get the difference in items of the storage box inventory view
            int after = findItems(slots).stream().mapToInt(ItemStack::getAmount).sum();
            int diff = after - before;

            // check if the storage box has enough space for these items
            if (after > before && storage.getAmount() + diff > storage.getCapacity()) {
                // evaluate leftovers
                int left = storage.getAmount() + diff - storage.getCapacity();
                leftover.accept(left);
                storage.setAmount(storage.getCapacity());

                // add leftovers to player inventory again
                ItemStack item = storage.getStored().clone();
                item.setAmount(left);
                adder.getInventory().addItem(item);
            } else {
                // update storage amount in storage box
                storage.setAmount(storage.getAmount() + diff);
            }
        });
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
