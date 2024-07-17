package dk.superawesome.factories.gui.impl;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.gui.MechanicGui;
import dk.superawesome.factories.mehcanics.Fuel;
import dk.superawesome.factories.mehcanics.impl.Smelter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SmelterGui extends MechanicGui<SmelterGui, Smelter> {

    private static final List<Integer> GRAY = Arrays.asList(0, 1, 2, 6, 7, 8, 42, 43, 44, 51, 53);
    private static final List<Integer> BLACK = Arrays.asList(5, 14, 23, 32, 41, 50);
    private static final List<Integer> RED = Arrays.asList(11, 20, 29, 38, 47);
    private static final List<Integer> INGREDIENT_SLOTS = Arrays.asList(9, 10, 18, 19, 27, 28, 36, 37, 45, 46);
    private static final List<Integer> FUEL_SLOTS = Arrays.asList(12, 13, 21, 22, 30, 31, 39, 40, 48, 49);
    private static final List<Integer> STORAGE_SLOTS = Arrays.asList(15, 16, 17, 24, 25, 26, 33, 34);

    public SmelterGui(Smelter mechanic, AtomicReference<SmelterGui> inUseReference) {
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
        for (int i = 3; i < 5; i++) {
            getInventory().setItem(i, new ItemStack(Material.FURNACE));
        }
        getInventory().setItem(35, new ItemStack(Material.FEATHER));

        updateFuelState();
    }

    @Override
    public void loadInputOutputItems() {
        if (getMechanic().getIngredient() != null) {
            loadStorageTypes(getMechanic().getIngredient(), getMechanic().getIngredientAmount(), INGREDIENT_SLOTS);
        }
        if (getMechanic().getFuel() != null) {
            loadStorageTypes(new ItemStack(getMechanic().getFuel().getMaterial()), getMechanic().getFuelAmount(), FUEL_SLOTS);
        }
        if (getMechanic().getStorageType() != null) {
            loadStorageTypes(getMechanic().getStorageType(), getMechanic().getStorageAmount(), STORAGE_SLOTS);
        }
    }

    @Override
    public void onClose() {

    }

    @Override
    public boolean onDrag(InventoryDragEvent event) {
        boolean modifyIngredients = event.getInventorySlots().stream().anyMatch(INGREDIENT_SLOTS::contains);
        boolean modifyFuel = event.getInventorySlots().stream().anyMatch(FUEL_SLOTS::contains);
        if (modifyFuel && modifyIngredients) {
            return true;
        }

        for (ItemStack item : event.getNewItems().values()) {
            if (modifyIngredients
                    && (!getMechanic().canSmelt(item.getType()) || handleInteractIngredient(item))) {
                return true;
            }

            if (modifyFuel
                    && (!Fuel.isFuel(item.getType()) || handleInteractFuel(item))) {
                return true;
            }
        }

        if (modifyIngredients) {
            updateIngredients();
        }

        if (modifyFuel) {
            updateFuel();
        }

        return false;
    }

    private boolean handleInteract(ItemStack cursor, Supplier<ItemStack> getItem, Consumer<ItemStack> setItem) {
        // check if a player tries to add an item to the storage box which is not the one currently being stored
        if (getItem.get() != null
                && cursor != null
                && cursor.getType() != Material.AIR
                && !getItem.get().isSimilar(cursor)) {
            return true;
        }

        if (getItem.get() == null
                && cursor != null
                && cursor.getType() != Material.AIR) {
            ItemStack stored = cursor.clone();
            stored.setAmount(1);
            setItem.accept(stored);
        }

        return false;
    }

    public void updateFuelState() {
        int blaze = Math.round(getMechanic().getCurrentFuelAmount() * 5f);
        AtomicInteger times = new AtomicInteger();
        IntStream.range(0, RED.size())
                .boxed()
                .map(RED::get)
                .sorted(Collections.reverseOrder())
                .forEach(i -> {
                     if (times.incrementAndGet() > blaze) {
                         getInventory().setItem(i, new ItemStack(Material.RED_STAINED_GLASS_PANE));
                         return;
                     }

                     getInventory().setItem(i, new ItemStack(Material.BLAZE_POWDER));
                });
    }

    private boolean handleInteractIngredient(ItemStack cursor) {
        return handleInteract(cursor, () -> getMechanic().getIngredient(), i -> getMechanic().setIngredient(i));
    }

    private boolean handleInteractFuel(ItemStack cursor) {
        Fuel fuel = getMechanic().getFuel();
        return handleInteract(cursor,
                () -> fuel == null ? null : new ItemStack(fuel.getMaterial()),
                i -> getMechanic().setFuel(Fuel.get(i.getType())));
    }

    private void updateAmount(List<Integer> slots, Consumer<Integer> applyDiff) {
        getMechanic().getTickThrottle().throttle();

        int before = find(slots).stream()
                .mapToInt(ItemStack::getAmount).sum();
        Bukkit.getScheduler().runTask(Factories.get(), () -> {
            int after = find(slots).stream()
                    .mapToInt(ItemStack::getAmount).sum();

            // get the difference in the items of the current inventory view of the storage box
            int diff = before - after;
            applyDiff.accept(diff);
        });
    }

    public void updateAddedStorage(int amount) {
        updateAddedItems(getInventory(), amount, getMechanic().getStorageType(), STORAGE_SLOTS);
    }

    public void updateAddedIngredients(int amount) {
        updateAddedItems(getInventory(), amount, getMechanic().getIngredient(), INGREDIENT_SLOTS);
    }

    public void updateAddedFuel(int amount) {
        updateAddedItems(getInventory(), amount, new ItemStack(getMechanic().getFuel().getMaterial()), FUEL_SLOTS);
    }

    public void updateRemovedStorage(int amount) {
        updateRemovedItems(getInventory(), amount, getMechanic().getStorageType(),
                IntStream.range(0, STORAGE_SLOTS.size())
                        .boxed()
                        .map(STORAGE_SLOTS::get)
                        .sorted(Collections.reverseOrder())
                        .collect(Collectors.toList()));
    }

    public void updateRemovedIngredients(int amount) {
        updateRemovedItems(getInventory(), amount, getMechanic().getIngredient(), INGREDIENT_SLOTS);
    }

    public void updateRemovedFuel(int amount) {
        updateRemovedItems(getInventory(), amount, new ItemStack(getMechanic().getFuel().getMaterial()), FUEL_SLOTS);
    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        if (INGREDIENT_SLOTS.contains(event.getSlot())) {
            if ((event.getCursor() != null && event.getCursor().getType() != Material.AIR && !getMechanic().canSmelt(event.getCursor().getType()))
                    || handleInteractIngredient(event.getCursor())) {
                return true;
            }

            updateIngredientsPost = true;
            return getMechanic().getTickThrottle().isThrottled();
        }

        if (FUEL_SLOTS.contains(event.getSlot())) {
            if ((event.getCursor() != null && event.getCursor().getType() != Material.AIR && !Fuel.isFuel(event.getCursor().getType()))
                    || handleInteractFuel(event.getCursor())) {
                return true;
            }

            updateFuelPost = true;
            return getMechanic().getTickThrottle().isThrottled();
        }

        if (event.getSlot() == 35) {
            loadInputOutputItems();
        }

        return true;
    }

    private List<ItemStack> find(List<Integer> slots) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            ItemStack item = getInventory().getItem(slots.get(i));
            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    @Override
    public boolean onClickOpen(InventoryClickEvent event) {
        if (movedFromOtherInventory(event)) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && event.getClickedInventory() != null) {

                if (event.getClickedInventory() == getInventory()) {
                    if (INGREDIENT_SLOTS.contains(event.getSlot())) {
                        updateIngredientsPost = true;
                    } else if (FUEL_SLOTS.contains(event.getSlot())) {
                        updateFuelPost = true;
                    }
                } else {
                    ItemStack item = event.getClickedInventory().getItem(event.getSlot());
                    if (item != null) {
                        int a = item.getAmount();
                        if (getMechanic().canSmelt(item.getType())
                                && !handleInteractIngredient(item)) {
                            addItemsToSlots(item, INGREDIENT_SLOTS);

                            getMechanic().setIngredientAmount(getMechanic().getIngredientAmount() + (a - item.getAmount()));
                        }

                        if ((a = item.getAmount()) > 0
                                && Fuel.isFuel(item.getType())
                                && !handleInteractFuel(item)) {
                            addItemsToSlots(item, FUEL_SLOTS);

                            getMechanic().setFuelAmount(getMechanic().getFuelAmount() + (a - item.getAmount()));
                        }

                        return true;
                    }
                }
            }

            if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR && event.getCursor() != null) {
                if (getMechanic().getIngredient() != null && event.getCursor().isSimilar(getMechanic().getIngredient())) {
                    updateIngredientsPost = true;
                }

                if (getMechanic().getFuel() != null && event.getCursor().getType() == getMechanic().getFuel().getMaterial()) {
                    updateFuelPost = true;
                }
            }

            if ((event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.HOTBAR_SWAP)) {
                ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());

                if ((updateIngredientsPost = INGREDIENT_SLOTS.contains(event.getSlot()))
                        && (find(INGREDIENT_SLOTS).size() > 1 || (hotbarItem != null && getMechanic().canSmelt(hotbarItem.getType())))
                        && handleInteractIngredient(hotbarItem)) {
                    updateIngredientsPost = false;
                    return true;
                }

                if ((updateFuelPost = FUEL_SLOTS.contains(event.getSlot()))
                        && (find(FUEL_SLOTS).size() > 1 || (hotbarItem != null && !Fuel.isFuel(hotbarItem.getType())))
                        && handleInteractFuel(hotbarItem)) {
                    updateFuelPost = false;
                    return true;
                }
            }

            return getMechanic().getTickThrottle().isThrottled();
        }

        return false;
    }

    private void updateIngredients() {
        updateAmount(INGREDIENT_SLOTS, diff -> {
            getMechanic().setIngredientAmount(getMechanic().getIngredientAmount() - diff);

            if (getMechanic().getIngredientAmount() == 0) {
                getMechanic().setIngredient(null);
                getMechanic().setSmeltResult(null);
            }
        });
    }

    private void updateFuel() {
        updateAmount(FUEL_SLOTS, diff -> {
            getMechanic().setFuelAmount(getMechanic().getFuelAmount() - diff);

            if (getMechanic().getFuelAmount() == 0) {
                getMechanic().setFuel(null);
            }
        });
    }

    private boolean updateIngredientsPost;
    private boolean updateFuelPost;

    @Override
    public void onClickPost(InventoryClickEvent event) {
        if (updateIngredientsPost) {
            updateIngredients();
        } else if (updateFuelPost) {
            updateFuel();
        }

        updateIngredientsPost = false;
        updateFuelPost = false;
    }
}
