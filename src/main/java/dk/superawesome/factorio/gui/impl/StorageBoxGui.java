package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.Elements;
import dk.superawesome.factorio.gui.GuiElement;
import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.impl.StorageBox;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StorageBoxGui extends MechanicGui<StorageBoxGui, StorageBox> {

    private static final int STORED_SIZE = 35;

    public StorageBoxGui(StorageBox mechanic, AtomicReference<StorageBoxGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadItems() {
        for (int i : Arrays.asList(45, 46, 47, 48, 50)) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        IntStream.range(36, 45).forEach(i -> getInventory().setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE)));
        getInventory().setItem(35, new ItemStack(Material.FEATHER));
        getInventory().setItem(49, new ItemStack(Material.MINECART));

        registerEvent(STORED_SIZE, __ -> loadInputOutputItems());
        registerEvent(49, this::handlePutOrTakeAll);

        super.loadItems();
    }

    @Override
    public void loadInputOutputItems() {
        if (getMechanic().getStored() != null) {
            loadStorageTypes(getMechanic().getStored(), getMechanic().getAmount(), IntStream.range(0, STORED_SIZE).boxed().collect(Collectors.toList()));
        }
    }

    public List<ItemStack> findItems(boolean asc) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = asc ? 0 : STORED_SIZE - 1; asc ? i < STORED_SIZE : i > -1; i += asc ? 1 : -1) {
            ItemStack item = getInventory().getItem(i);
            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    public List<ItemStack> findItems() {
        return findItems(true);
    }

    public void updateAddedItems(int amount) {
        updateAddedItems(getInventory(), amount, getMechanic().getStored(),
                IntStream.range(0, STORED_SIZE)
                        .boxed()
                        .collect(Collectors.toList()));
    }

    public void updateRemovedItems(int amount) {
        updateRemovedItems(getInventory(), amount, getMechanic().getStored(),
                IntStream.range(0, STORED_SIZE)
                        .boxed()
                        .sorted(Collections.reverseOrder())
                        .collect(Collectors.toList()));
    }

    @Override
    public void onClose(Player player) {

    }

    private void updateAmount(HumanEntity adder) {
        getMechanic().getTickThrottle().throttle();

        int before = findItems().stream().mapToInt(ItemStack::getAmount).sum();
        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
            // get the difference in items of the storage box inventory view
            int after = findItems().stream().mapToInt(ItemStack::getAmount).sum();
            int diff = after - before;

            // check if the storage box has enough space for these items
            if (after > before && getMechanic().getAmount() + diff > getMechanic().getCapacity()) {
                // evaluate leftovers
                int left = getMechanic().getAmount() + diff - getMechanic().getCapacity();
                updateRemovedItems(left);
                getMechanic().setAmount(getMechanic().getCapacity());

                // add leftovers to player inventory again
                ItemStack item = getMechanic().getStored().clone();
                item.setAmount(left);
                adder.getInventory().addItem(item);
            } else {
                // update storage amount in storage box
                getMechanic().setAmount(getMechanic().getAmount() + diff);
            }
        });
    }

    private boolean handleInteract(ItemStack cursor) {
        // check if a player tries to add an item to the storage box which is not the one currently being stored
        if (getMechanic().getStored() != null
                && cursor != null
                && cursor.getType() != Material.AIR
                && !cursor.isSimilar(getMechanic().getStored())) {
            return true;
        }

        // update stored stack
        if (getMechanic().getStored() == null
                && cursor != null
                && cursor.getType() != Material.AIR) {
            ItemStack stored = cursor.clone();
            stored.setAmount(1);
            getMechanic().setStored(stored);
        }

        return false;
    }

    @Override
    public boolean onDrag(InventoryDragEvent event) {
        if (getMechanic().getTickThrottle().isThrottled()) {
            return true;
        }

        // check if all slots dragged over are just in the player's own inventory
        if (event.getRawSlots().stream().allMatch(s -> event.getView().getInventory(s).getType() == InventoryType.PLAYER)
                // check if none of the slots are in the storage box view
                || event.getRawSlots().stream().noneMatch(i -> i < STORED_SIZE)) {
            // ... if it was, don't continue
            return false;
        }

        int amount = getMechanic().getAmount();
        if (amount == getMechanic().getCapacity()) {
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
        boolean checkSize = amount + added > getMechanic().getCapacity();

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
            if (handleInteract(item)) {
                return true;
            }

            // register added item
            amount += item.getAmount();
            if (at != null) {
                // the item at this slot originally, wasn't added in this event
                amount -= at.getAmount();
            }

            // check if the storage box does not have enough capacity for these items
            if (checkSize && amount > getMechanic().getCapacity()) {
                int subtract = amount - getMechanic().getCapacity();
                amount -= subtract;
                added -= subtract;
                item.setAmount(item.getAmount() - subtract);

                cancel = true;
            }
        }

        // update storage amount for storage box
        getMechanic().setAmount(amount);

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

    private void handlePutOrTakeAll(InventoryClickEvent event) {
        Inventory playerInv = event.getWhoClicked().getInventory();

        if (event.getClick() == ClickType.LEFT) {
            if (getMechanic().getStored() == null) {
                // the storage box does not have any stored item
                // find the item type in the player inventory which occurs the most
                Map<Material, Integer> typeAmounts = new HashMap<>();
                for (int i = 0; i < playerInv.getSize(); i++) {
                    ItemStack item = playerInv.getItem(i);
                    if (item != null) {
                        typeAmounts.put(item.getType(), typeAmounts.getOrDefault(item.getType(), 0) + item.getAmount());
                    }
                }

                if (typeAmounts.isEmpty()) {
                    // empty inventory
                    return;
                }

                Material highest = null;
                for (Map.Entry<Material, Integer> entry : typeAmounts.entrySet()) {
                    if (highest == null || entry.getValue() > typeAmounts.get(highest)) {
                        highest = entry.getKey();
                    }
                }

                // update the stored item stack
                if (highest != null) {
                    int amount = Math.min(getMechanic().getCapacity(), typeAmounts.get(highest));
                    getMechanic().setStored(new ItemStack(highest));
                    getMechanic().setAmount(amount);

                    updateRemovedItems(playerInv, amount, getMechanic().getStored(),
                            IntStream.range(0, playerInv.getSize())
                                    .boxed()
                                    .collect(Collectors.toList()));
                    updateAddedItems(amount);
                }
            } else if (getMechanic().getAmount() < getMechanic().getCapacity()) {
                int add = getMechanic().getCapacity() - getMechanic().getAmount();
                // take all items from the player's inventory and put into the storage box
                int left = updateRemovedItems(playerInv, add, getMechanic().getStored(),
                        IntStream.range(0, playerInv.getSize())
                                .boxed()
                                .collect(Collectors.toList()));
                int amount = add - left;

                // update amount if we were able to add anything
                if (amount > 0) {
                    getMechanic().setAmount(getMechanic().getAmount() + amount);
                    updateAddedItems(amount);
                }
            }
        } else if (event.getClick() == ClickType.RIGHT) {
            if (getMechanic().getStored() == null) {
                // no items stored, nothing can be collected
                return;
            }

            // put all items we can in the player's inventory from the storage box
            int left = updateAddedItems(playerInv, getMechanic().getAmount(), getMechanic().getStored(),
                    IntStream.range(0, playerInv.getSize())
                            .boxed()
                            .collect(Collectors.toList()));
            int amount = getMechanic().getAmount() - left;

            if (amount == 0) {
                // no items could be added to the player's inventory
                return;
            }

            getMechanic().setAmount(getMechanic().getAmount() - amount);
            updateRemovedItems(amount);
        }
    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        if (event.getRawSlot() < STORED_SIZE) {
            return handleInteract(event.getCursor());
        }

        return true;
    }

    @Override
    public boolean onClickOpen(InventoryClickEvent event) {
        if (movedFromOtherInventory(event)) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    && event.getClickedInventory() != getInventory()
                    && handleInteract(event.getCurrentItem())) {
                return true;
            }
            else if ((event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.HOTBAR_SWAP)
                    && (event.getCurrentItem() == null || findItems().size() > 1)
                    && handleInteract(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onClickPost(InventoryClickEvent event) {
        if (getMechanic().getTickThrottle().isThrottled()) {
            event.setCancelled(true);
        }

        if (!event.isCancelled()) {
            updateAmount(event.getWhoClicked());
        }
    }
}
