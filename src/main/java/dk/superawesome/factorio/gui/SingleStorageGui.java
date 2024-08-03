package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.SingleStorage;
import dk.superawesome.factorio.util.Callback;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class SingleStorageGui<G extends BaseGui<G>, M extends Mechanic<M, G>> extends MechanicGui<G, M> {
    
    private final List<Integer> slots;
    
    public SingleStorageGui(M mechanic, AtomicReference<G> inUseReference, Supplier<Callback> initCallback, List<Integer> slots) {
        super(mechanic, inUseReference, initCallback);
        this.slots = slots;
    }
    
    public abstract SingleStorage getStorage();

    protected abstract boolean isItemAllowed(ItemStack item);

    @Override
    public void loadInputOutputItems() {
        if (getStorage() != null) {
            loadStorageTypes(getStorage().getStored(), getStorage().getAmount(), slots);
        }
    }

    public List<ItemStack> findItems() {
        List<ItemStack> items = new ArrayList<>();
        for (int i : slots) {
            ItemStack item = getInventory().getItem(i);
            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    public void updateAddedItems(int amount) {
        updateAddedItems(getInventory(), amount, getStorage().getStored(), slots);
    }

    public void updateRemovedItems(int amount) {
        updateRemovedItems(getInventory(), amount, getStorage().getStored(), slots.stream().sorted(Collections.reverseOrder()).collect(Collectors.toList()));
    }

    private void updateAmount(HumanEntity adder) {
        getMechanic().getTickThrottle().throttle();

        int before = findItems().stream().mapToInt(ItemStack::getAmount).sum();
        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
            // get the difference in items of the storage box inventory view
            int after = findItems().stream().mapToInt(ItemStack::getAmount).sum();
            int diff = after - before;

            // check if the storage box has enough space for these items
            if (after > before && getStorage().getAmount() + diff > getStorage().getCapacity()) {
                // evaluate leftovers
                int left = getStorage().getAmount() + diff - getStorage().getCapacity();
                updateRemovedItems(left);
                getStorage().setAmount(getStorage().getCapacity());

                // add leftovers to player inventory again
                ItemStack item = getStorage().getStored().clone();
                item.setAmount(left);
                adder.getInventory().addItem(item);
            } else {
                // update storage amount in storage box
                getStorage().setAmount(getStorage().getAmount() + diff);
            }
        });
    }

    private boolean checkFailedInteraction(ItemStack item) {
        // check if a player tries to add an item to the storage box which is not the one currently being stored
        if (getStorage().getStored() != null
                && item != null
                && item.getType() != Material.AIR
                && !item.isSimilar(getStorage().getStored())) {
            return true;
        }

        // check if this item is allowed to be stored
        if (!isItemAllowed(item)) {
            return true;
        }

        // update stored stack
        if (getStorage().getStored() == null
                && item != null
                && item.getType() != Material.AIR) {
            ItemStack stored = item.clone();
            stored.setAmount(1);
            getStorage().setStored(stored);
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
                || event.getRawSlots().stream().noneMatch(slots::contains)) {
            // ... if it was, don't continue
            return false;
        }

        int amount = getStorage().getAmount();
        if (amount == getStorage().getCapacity()) {
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
        boolean checkSize = amount + added > getStorage().getCapacity();

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
            if (checkFailedInteraction(item)) {
                return true;
            }

            // register added item
            amount += item.getAmount();
            if (at != null) {
                // the item at this slot originally, wasn't added in this event
                amount -= at.getAmount();
            }

            // check if the storage box does not have enough capacity for these items
            if (checkSize && amount > getStorage().getCapacity()) {
                int subtract = amount - getStorage().getCapacity();
                amount -= subtract;
                added -= subtract;
                item.setAmount(item.getAmount() - subtract);

                cancel = true;
            }
        }

        // update storage amount for storage box
        getStorage().setAmount(amount);

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

    private Material findHighestItemCount(Inventory inventory, Map<Material, Integer> typeAmounts) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                typeAmounts.put(item.getType(), typeAmounts.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }

        if (typeAmounts.isEmpty()) {
            // empty inventory
            return null;
        }

        Material highest = null;
        // loop through all entries and find the highest value
        for (Map.Entry<Material, Integer> entry : typeAmounts.entrySet()) {
            if (highest == null || entry.getValue() > typeAmounts.get(highest)) {
                highest = entry.getKey();
            }
        }

        return highest;
    }

    protected void handlePutOrTakeAll(InventoryClickEvent event) {
        Inventory playerInv = event.getWhoClicked().getInventory();

        if (event.getClick().isLeftClick()) {
            // create a put function
            Consumer<Double> put = a -> {
                int space = Math.min(getStorage().getCapacity() - getStorage().getAmount(), (int) a.doubleValue());
                // take all items from the player's inventory and put into the storage box
                int left = updateRemovedItems(playerInv, space, getStorage().getStored(),
                        IntStream.range(0, playerInv.getSize())
                                .boxed()
                                .collect(Collectors.toList()));
                int amount = space - left;

                // update amount if we were able to add anything
                if (amount > 0) {
                    getStorage().setAmount(getStorage().getAmount() + amount);
                    updateAddedItems(amount);
                }
            };

            Material stored = null;
            if (getStorage().getStored() == null) {
                // the storage box does not have any stored item
                // find the item type in the player inventory which occurs the most
                Map<Material, Integer> typeAmounts = new HashMap<>();
                Material highest = findHighestItemCount(playerInv, typeAmounts);

                // update the stored item stack
                if (highest != null) {
                    if (event.getClick().isShiftClick()) {
                        // update the stored amount and put into the storage box at (1)
                        getStorage().setStored(new ItemStack(highest));
                    } else {
                        stored = highest;
                        Consumer<Double> putCopy = put;
                        put = i -> {
                            if (getStorage().getStored() == null) {
                                // the stored type has not changed while the player was editing the sign
                                // just assume the highest item count is the same as when the sign was opened
                                getStorage().setStored(new ItemStack(highest));
                            }
                            putCopy.accept(i);
                        };
                    }
                }
            } else if (getStorage().getAmount() >= getStorage().getCapacity()) {
                return;
            }

            if (stored == null && getStorage().getStored() != null) {
                stored = getStorage().getStored().getType();
            }

            if (stored != null) {
                // (1)
                if (event.getClick().isShiftClick()) {
                    put.accept((double) getStorage().getCapacity() - getStorage().getAmount());
                } else {
                    Material storedCopy = stored;
                    // evaluate total amount of the storage box stored item present in the player's inventory
                    int amount = Arrays.stream(playerInv.getContents())
                            .filter(Objects::nonNull)
                            .filter(i -> i.getType().equals(storedCopy))
                            .mapToInt(ItemStack::getAmount)
                            .sum();
                    if (amount > 0) {
                        openSignGuiAndCall((Player) event.getWhoClicked(), amount + "", put);
                    }
                }
            }
        } else if (event.getClick().isRightClick()) {
            if (getStorage().getStored() == null) {
                // no items stored, nothing can be collected
                return;
            }

            // create a take function
            Consumer<Double> take = a -> {
                int boxAmount = (int) Math.min(a, getStorage().getAmount());
                // put all items we can in the player's inventory from the storage box
                int left = updateAddedItems(playerInv, boxAmount, getStorage().getStored(),
                        IntStream.range(0, playerInv.getSize())
                                .boxed()
                                .collect(Collectors.toList()));
                int amount = boxAmount - left;
                if (amount == 0) {
                    // no items could be added to the player's inventory
                    return;
                }

                updateRemovedItems(amount);
                getStorage().setAmount(getStorage().getAmount() - amount);
            };

            if (event.getClick().isShiftClick()) {
                take.accept((double) getStorage().getAmount());
            } else {
                openSignGuiAndCall((Player) event.getWhoClicked(), getStorage().getAmount() + "", take);
            }
        }
    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        if (slots.contains(event.getRawSlot()))  {
            return checkFailedInteraction(event.getCursor());
        }

        return true;
    }

    @Override
    public boolean onClickOpen(InventoryClickEvent event) {
        if (movedFromOtherInventory(event)) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    && event.getClickedInventory() != getInventory()
                    && checkFailedInteraction(event.getCurrentItem())) {
                return true;
            }
            else if ((event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.HOTBAR_SWAP)
                    && (event.getCurrentItem() == null || findItems().size() > 1)
                    && checkFailedInteraction(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()))) {
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
