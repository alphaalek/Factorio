package dk.superawesome.factories.gui.impl;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.gui.MechanicGui;
import dk.superawesome.factories.mechanics.impl.StorageBox;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StorageBoxGui extends MechanicGui<StorageBoxGui, StorageBox> {

    private static final int STORED_SIZE = 35;
    private static final List<Integer> GRAY = Arrays.asList(45, 46, 47, 51, 53);
    private static final List<Integer> BLACK = IntStream.range(36, 45).boxed().collect(Collectors.toList());

    static {
        BLACK.addAll(Arrays.asList(48, 50));
    }

    public StorageBoxGui(StorageBox mechanic, AtomicReference<StorageBoxGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadItems() {
        super.loadItems();

        for (int i : GRAY) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        for (int i : BLACK) {
            getInventory().setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        }
        getInventory().setItem(35, new ItemStack(Material.FEATHER));
        getInventory().setItem(49, new ItemStack(Material.MINECART));
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
    public void onClose() {

    }

    private void updateAmount(HumanEntity adder) {
        getMechanic().getTickThrottle().throttle();

        int before = findItems().stream()
                .mapToInt(ItemStack::getAmount).sum();
        Bukkit.getScheduler().runTask(Factories.get(), () -> {
            // get the difference in items of the storage box inventory view
            int after = findItems().stream()
                    .mapToInt(ItemStack::getAmount).sum();
            int diff = after - before;

            Bukkit.broadcastMessage(after + " " + before + " " + diff + " " + getMechanic().getAmount());
            // check if the storage box has enough space for these items
            if (after > before && getMechanic().getAmount() + diff > getMechanic().getCapacity()) {
                // evaluate leftovers
                getMechanic().setAmount(getMechanic().getCapacity());
                int left = getMechanic().getAmount() + diff - getMechanic().getCapacity();
                updateRemovedItems(left);

                // add leftovers to inventory again
                ItemStack item = getMechanic().getStored().clone();
                item.setAmount(left);
                Bukkit.getLogger().info("Left " + left);
                adder.getInventory().addItem(item);
            } else {
                // update storage amount in storage box
                getMechanic().setAmount(getMechanic().getAmount() + diff);

                if (getMechanic().getAmount() == 0) {
                    getMechanic().setStored(null);
                }
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

        // TODO fix bugs with storage update

        // only check for storage slots
        if (event.getInventorySlots().stream().anyMatch(i -> i < 35)) {
            int capacity = getMechanic().getCapacity();
            int amount = getMechanic().getAmount();
            if (amount == capacity) {
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
            boolean checkSize = amount + added > capacity;

            boolean cancel = false;
            // add the dragged items
            for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
                ItemStack item = entry.getValue();
                if (handleInteract(item)) {
                    return true;
                }

                amount += item.getAmount();
                ItemStack at = event.getView().getItem(entry.getKey());
                if (at != null) {
                    // the item at this slot originally, wasn't added in this event
                    amount -= at.getAmount();
                }

                // check if the storage box does not have enough capacity for these items
                if (checkSize) {
                    if (amount > capacity) {
                        int subtract = amount - capacity;
                        amount -= subtract;
                        added -= subtract;
                        item.setAmount(item.getAmount() - subtract);

                        // ensure cursor set for post-work
                        ItemStack cursor = event.getCursor();
                        if (cursor == null) {
                            cursor = item.clone();
                        }
                        event.setCursor(cursor);

                        cancel = true;
                    }
                }
            }

            // update storage amount for storage box
            getMechanic().setAmount(amount);

            // do manual work if there was too many items tried to be added
            if (cancel) {
                getMechanic().getTickThrottle().throttle();

                // re-set items
                Bukkit.getScheduler().runTask(Factories.get(), () -> {
                    for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
                        event.getView().setItem(entry.getKey(), entry.getValue());
                    }
                });

                // remove the added items from the cursor
                if (event.getCursor() != null) {
                    event.getCursor().setAmount(event.getCursor().getAmount() - added);

                    Bukkit.getScheduler().runTask(Factories.get(), () -> {
                        event.getWhoClicked().getOpenInventory().setCursor(event.getCursor());
                    });
                }

                return true;
            }

            return false;
        }

        return true;
    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        if (event.getSlot() < 35) {
            if (handleInteract(event.getCursor())) {
                return true;
            }

            return getMechanic().getTickThrottle().isThrottled();
        }

        if (event.getSlot() == 35) {
            loadInputOutputItems();
        }

        if (event.getSlot() == 49) {
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
                        return true;
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
                    // take all items from the player's inventory and put into the storage box
                    int left = updateRemovedItems(playerInv, Integer.MAX_VALUE, getMechanic().getStored(),
                            IntStream.range(0, playerInv.getSize())
                                    .boxed()
                                    .collect(Collectors.toList()));
                    int amount = Integer.MAX_VALUE - left;
                    if (getMechanic().getAmount() + amount > getMechanic().getCapacity()) {
                        amount -= getMechanic().getAmount() + amount - getMechanic().getCapacity();
                    }

                    getMechanic().setAmount(getMechanic().getAmount() + amount);
                    updateAddedItems(amount);
                }
            } else if (event.getClick() == ClickType.RIGHT) {
                if (getMechanic().getStored() == null) {
                    // no items stored, nothing can be collected
                    return true;
                }

                // put all items we can in the player's inventory from the storage box
                int left = updateAddedItems(playerInv, getMechanic().getAmount(), getMechanic().getStored(),
                        IntStream.range(0, playerInv.getSize())
                                .boxed()
                                .collect(Collectors.toList()));
                int amount = getMechanic().getAmount() - left;

                if (amount == 0) {
                    // no items could be added to the player's inventory
                    return true;
                }

                getMechanic().setAmount(getMechanic().getAmount() - amount);
                updateRemovedItems(amount);
            }
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

            if ((event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.HOTBAR_SWAP)
                    && (event.getCurrentItem() == null || findItems().size() > 1)
                    && handleInteract(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()))) {
                return true;
            }

            return getMechanic().getTickThrottle().isThrottled();
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
