package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.Storage;
import dk.superawesome.factorio.util.Callback;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class SingleStorageGui<G extends BaseGui<G>, M extends Mechanic<M>> extends MechanicGui<G, M> implements MechanicStorageGui {

    public static final int CONTEXT = 0;

    private final List<Integer> slots;
    private final Storage storage;
    
    public SingleStorageGui(M mechanic, AtomicReference<G> inUseReference, Supplier<Callback> initCallback) {
        super(mechanic, inUseReference, initCallback);
        this.slots = mechanic.getProfile().getStorageProvider().getSlots(getContext());
        storage = mechanic.getProfile().getStorageProvider().createStorage(mechanic, getContext());
    }
    
    public abstract int getContext();

    protected abstract boolean isItemAllowed(ItemStack item);

    @Override
    public void updateItems() {
        if (storage.getStored() != null) {
            loadStorageTypes(storage.getStored(), storage.getAmount(), slots);
        }
        updateStorageInfo();
    }

    @Override
    public int getInputContext() {
        return CONTEXT;
    }

    @Override
    public int getOutputContext() {
        return CONTEXT;
    }

    @Override
    public MechanicGui<?, ?> getMechanicGui() {
        return this;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public void updateAddedItems(int amount) {
        updateAddedItems(getInventory(), amount, storage.getStored(), slots);
    }

    public void updateRemovedItems(int amount) {
        updateRemovedItems(getInventory(), amount, storage.getStored(), slots.stream().sorted(Collections.reverseOrder()).collect(Collectors.toList()));
    }

    private boolean registerInteractionAndCheckFailed(ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            // check if a player tries to add an item to the storage box which is not the one currently being stored
            if (storage.getStored() != null && !item.isSimilar(storage.getStored())
                    // check if this item is allowed to be stored
                    || !isItemAllowed(item)) {
                return true;
            }

            // update stored stack
            if (storage.getStored() == null) {
                ItemStack stored = item.clone();
                stored.setAmount(1);
                storage.setStored(stored);
            }
        }

        return false;
    }

    @Override
    public boolean onDrag(InventoryDragEvent event) {
        if (getMechanic().getTickThrottle().isThrottled()) {
            return true;
        }

        // check if all slots dragged over are in the storage view
        if (!event.getRawSlots().stream().allMatch(s -> slots.contains(s)
                || event.getView().getInventory(s).equals(event.getWhoClicked().getOpenInventory().getBottomInventory()))) {
            // ... if it was, don't continue
            return true;
        }

        int amount = storage.getAmount();
        if (amount == storage.getCapacity()) {
            return true;
        }

        // check if the amount of items in the storage box will exceed the capacity
        int added = event.getNewItems().entrySet().stream()
                .mapToInt(entry -> entry.getValue().getAmount() -
                        Optional.ofNullable(event.getView().getItem(entry.getKey()))
                                .map(ItemStack::getAmount)
                                .orElse(0)
                )
                .sum();
        boolean checkSize = amount + added > storage.getCapacity();

        ItemStack cursor = event.getOldCursor();
        boolean cancel = false;
        // add the dragged items
        for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
            ItemStack item = entry.getValue();
            ItemStack at = event.getView().getItem(entry.getKey());

            // do not register added items to the storage box if the slot is in the player's own inventory
            if (event.getView().getInventory(entry.getKey()).getType() == InventoryType.PLAYER) {
                continue;
            }

            // check if this item can be added to the storage box
            if (registerInteractionAndCheckFailed(item)) {
                return true;
            }

            // register added item
            amount += item.getAmount();
            if (at != null) {
                // the item at this slot originally, wasn't added in this event
                amount -= at.getAmount();
            }

            // check if the storage box does not have enough capacity for these items
            if (checkSize && amount > storage.getCapacity()) {
                int subtract = amount - storage.getCapacity();
                amount -= subtract;
                added -= subtract;
                item.setAmount(item.getAmount() - subtract);

                cancel = true;
            }
        }

        // update storage amount for storage box
        storage.setAmount(amount);

        // do manual work if there was too many items tried to be added
        if (cancel) {
            getMechanic().getTickThrottle().throttle();

            // re-set items
            Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
                    event.getView().setItem(entry.getKey(), entry.getValue());
                }
            });

            // remove the added items from the cursor
            int a = added;
            Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                cursor.setAmount(cursor.getAmount() - a);
                event.getWhoClicked().getOpenInventory().setCursor(cursor);
            });

            return true;
        }

        return false;
    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        if (slots.contains(event.getRawSlot()))  {
            return registerInteractionAndCheckFailed(event.getCursor());
        }

        return true;
    }

    @Override
    public boolean onClickOpen(InventoryClickEvent event) {
        if (movedFromOtherInventory(event)) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    && event.getClickedInventory() != getInventory()
                    && event.getCurrentItem() != null) {
                fixPutSlots = true;
                return registerInteractionAndCheckFailed(event.getCurrentItem());
            } else if ((event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.HOTBAR_SWAP)
                    && (event.getCurrentItem() == null || findItems(slots).size() > 1)
                    && registerInteractionAndCheckFailed(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()))) {
                return true;
            } else if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR
                    && event.getClickedInventory() != getInventory()
                    && !event.getCursor().isSimilar(storage.getStored())) {
                return true;
            }
        }

        return false;
    }

    private boolean fixPutSlots;

    @Override
    public void onClickPost(InventoryClickEvent event) {
        if (getMechanic().getTickThrottle().isThrottled()) {
            event.setCancelled(true);
        }

        if (!event.isCancelled()) {
            updateAmount(storage, event.getWhoClicked().getInventory(), slots, this::updateRemovedItems);

            if (fixPutSlots) {
                // InventoryClickEvent#getCurrentItem can't be null in this case
                fixSlotsPut(event.getCurrentItem(), event.getClickedInventory(), slots);
            }
        }

        fixPutSlots = false;
    }
}
