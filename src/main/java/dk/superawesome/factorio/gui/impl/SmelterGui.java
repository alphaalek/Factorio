package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.items.Fuel;
import dk.superawesome.factorio.mechanics.impl.Smelter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class SmelterGui extends MechanicGui<SmelterGui, Smelter> {

    private static final List<Integer> INGREDIENT_SLOTS = Arrays.asList(9, 10, 18, 19, 27, 28, 36, 37, 45, 46);
    private static final List<Integer> FUEL_SLOTS = Arrays.asList(12, 13, 21, 22, 30, 31, 39, 40, 48, 49);
    private static final List<Integer> STORAGE_SLOTS = Arrays.asList(6, 7, 8, 15, 16, 17, 24, 25, 26, 33, 34);

    public SmelterGui(Smelter mechanic, AtomicReference<SmelterGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadItems() {
        for (int i : Arrays.asList(0, 1, 2, 5, 14, 23, 32, 41, 42, 43, 44, 50)) {
            getInventory().setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        }
        for (int i = 3; i < 5; i++) {
            getInventory().setItem(i, new ItemStack(Material.FURNACE));
        }
        getInventory().setItem(35, new ItemStack(Material.FEATHER));

        updateFuelState();
        if (getMechanic().isDeclined()) {
            updateDeclinedState(true);
        }

        super.loadItems();
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

    public void updateDeclinedState(boolean declined) {
        if (declined) {
            for (int i : Arrays.asList(14, 23, 32)) {
                getInventory().setItem(i, new ItemStack(Material.BARRIER));
            }
        } else {
            for (int i : Arrays.asList(14, 23, 32)) {
                getInventory().setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
            }
        }
    }

    private void updateType(ItemStack item, Consumer<ItemStack> set) {
        ItemStack stored = item.clone();
        stored.setAmount(1);
        set.accept(stored);
    }

    public void updateFuelState() {
        int blaze = Math.round(getMechanic().getCurrentFuelAmount() * 5f);
        AtomicInteger times = new AtomicInteger();
        Stream.of(11, 20, 29, 38, 47)
                .sorted(Collections.reverseOrder())
                .forEach(i -> {
                    if (times.incrementAndGet() > blaze) {
                        getInventory().setItem(i, new ItemStack(Material.RED_STAINED_GLASS_PANE));
                        return;
                    }

                    getInventory().setItem(i, new ItemStack(Material.BLAZE_POWDER));
                });
    }

    private void updateAmount(List<Integer> slots, Consumer<Integer> applyDiff) {
        getMechanic().getTickThrottle().throttle();

        int before = find(slots).stream()
                .mapToInt(ItemStack::getAmount).sum();
        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
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
        updateRemovedItems(getInventory(), amount, getMechanic().getStorageType(), reverseSlots(STORAGE_SLOTS));
    }

    public void updateRemovedIngredients(int amount) {
        updateRemovedItems(getInventory(), amount, getMechanic().getIngredient(), reverseSlots(INGREDIENT_SLOTS));
    }

    public void updateRemovedFuel(int amount) {
        updateRemovedItems(getInventory(), amount, new ItemStack(getMechanic().getFuel().getMaterial()), reverseSlots(FUEL_SLOTS));
    }

    @Override
    public void onClose(Player player) {

    }

    @Override
    public boolean onDrag(InventoryDragEvent event) {
        if (getMechanic().getTickThrottle().isThrottled()
                && event.getRawSlots().stream().anyMatch(s -> event.getView().getInventory(s).getType() != InventoryType.PLAYER)) {
            return true;
        }

        boolean modifyIngredients = event.getRawSlots().stream().anyMatch(INGREDIENT_SLOTS::contains);
        boolean modifyFuel = event.getRawSlots().stream().anyMatch(FUEL_SLOTS::contains);
        // disallow dragging items over both the ingredient and fuel slots
        if (modifyFuel && modifyIngredients) {
            return true;
        }

        for (ItemStack item : event.getNewItems().values()) {
            // disallow if ingredient is not allowed
            if (modifyIngredients
                    && getMechanic().getIngredient() != null && !getMechanic().getIngredient().isSimilar(item) || getMechanic().getIngredient() == null && !getMechanic().canSmelt(item.getType())) {
                return true;
            }

            // disallow if fuel is not allowed
            if (modifyFuel
                    && (getMechanic().getFuel() != null && getMechanic().getFuel().getMaterial() != item.getType() || getMechanic().getFuel() == null && !Fuel.isFuel(item.getType()))) {
                return true;
            }
        }

        // this was not dragged over either the ingredient or fuel slots, don't continue
        if (!modifyFuel && !modifyIngredients) {
            return true;
        }

        ItemStack item = event.getNewItems().entrySet().iterator().next().getValue();
        if (modifyIngredients) {
            // update ingredient if not set
            if (getMechanic().getIngredient() == null) {
                updateType(item, i -> getMechanic().setIngredient(i));
            }

            updateIngredients();
        }

        if (modifyFuel) {
            // update fuel if not set
            if (getMechanic().getFuel() == null) {
                getMechanic().setFuel(Fuel.getFuel(item.getType()));
            }

            updateFuel();
        }

        return false;
    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        if (INGREDIENT_SLOTS.contains(event.getRawSlot())) {
            if (event.getCursor() == null || event.getCursor().getType() == Material.AIR) {
                updateIngredientsPost =  true;
                return false;
            }

            if ((event.getCursor() != null && event.getCursor().getType() != Material.AIR)) {
                if (getMechanic().getIngredient() != null && getMechanic().getIngredient().isSimilar(event.getCursor()) || getMechanic().getIngredient() == null && getMechanic().canSmelt(event.getCursor().getType())) {
                    updateIngredientsPost = true;

                    // update ingredient if not set
                    if (getMechanic().getIngredient() == null) {
                        updateType(event.getCursor(), i -> getMechanic().setIngredient(i));
                    }

                    return false;
                }
            }

            return true;
        }

        if (FUEL_SLOTS.contains(event.getRawSlot())) {
            if (event.getCursor() == null || event.getCursor().getType() == Material.AIR) {
                updateFuelPost = true;
                return false;
            }

            if ((event.getCursor() != null && event.getCursor().getType() != Material.AIR)) {
                if (getMechanic().getFuel() != null && getMechanic().getFuel().getMaterial() == event.getCursor().getType() || getMechanic().getFuel() == null && Fuel.isFuel(event.getCursor().getType())) {
                    updateFuelPost = true;

                    // update fuel if not set
                    if (getMechanic().getFuel() == null) {
                        getMechanic().setFuel(Fuel.getFuel(event.getCursor().getType()));
                    }

                    return false;
                }
            }

            return true;
        }

        if (event.getRawSlot() == 35) {
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
                    if (INGREDIENT_SLOTS.contains(event.getRawSlot())) {
                        updateIngredientsPost = true;
                    } else if (FUEL_SLOTS.contains(event.getRawSlot())) {
                        updateFuelPost = true;
                    }
                } else if (!getMechanic().getTickThrottle().isThrottled()) {
                    ItemStack item = event.getClickedInventory().getItem(event.getSlot());
                    if (item != null) {
                        ItemStack copy = item.clone();
                        int a = item.getAmount();
                        if (getMechanic().getIngredient() != null && getMechanic().getIngredient().isSimilar(item) || getMechanic().getIngredient() == null && getMechanic().canSmelt(item.getType())) {
                            addItemsToSlots(item, INGREDIENT_SLOTS);
                            getMechanic().setIngredientAmount(getMechanic().getIngredientAmount() + (a - item.getAmount()));

                            // update smelt result if not set
                            if (getMechanic().getSmeltResult() == null) {
                                getMechanic().setSmeltResult(getMechanic().getCachedSmeltResult());
                            }

                            // update ingredient if not set
                            if (getMechanic().getIngredient() == null) {
                                updateType(copy, i -> getMechanic().setIngredient(i));
                            }
                        }

                        if ((a = item.getAmount()) > 0
                                && getMechanic().getFuel() != null && getMechanic().getFuel().getMaterial() == copy.getType() || getMechanic().getFuel() == null && Fuel.isFuel(item.getType())) {
                            addItemsToSlots(item, FUEL_SLOTS);
                            getMechanic().setFuelAmount(getMechanic().getFuelAmount() + (a - item.getAmount()));

                            // update fuel if not set
                            if (getMechanic().getFuel() == null) {
                                getMechanic().setFuel(Fuel.getFuel(copy.getType()));
                            }
                        }

                        return true;
                    }
                }
            }

            if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR && event.getCursor() != null) {
                if (getMechanic().getIngredient() != null
                        && event.getCursor().isSimilar(getMechanic().getIngredient())) {
                    updateIngredientsPost = true;
                }

                if (getMechanic().getFuel() != null
                        && event.getCursor().getType() == getMechanic().getFuel().getMaterial()) {
                    updateFuelPost = true;
                }
            }

            if ((event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.HOTBAR_SWAP)) {
                ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());

                if (INGREDIENT_SLOTS.contains(event.getRawSlot())) {
                    if (hotbarItem == null) {
                        updateIngredientsPost = true;
                        return false;
                    }

                    // check if this action can be performed
                    if (find(INGREDIENT_SLOTS).size() <= 1 && getMechanic().canSmelt(hotbarItem.getType())) {
                        updateIngredientsPost = true;

                        // update ingredient if not set or not equal
                        if (getMechanic().getIngredient() == null || !getMechanic().getIngredient().isSimilar(hotbarItem)) {
                            updateType(hotbarItem, i -> getMechanic().setIngredient(i));
                        }

                        // update smelt result if not set or not equal
                        if (getMechanic().getSmeltResult() == null || !getMechanic().getSmeltResult().isSimilar(hotbarItem)) {
                            getMechanic().setSmeltResult(getMechanic().getCachedSmeltResult());
                        }

                        return false;
                    }
                }

                if (FUEL_SLOTS.contains(event.getRawSlot())) {
                    if (hotbarItem == null) {
                        updateFuelPost = true;
                        return false;
                    }

                    // check if this action can be performed
                    if (find(FUEL_SLOTS).size() <= 1 && Fuel.isFuel(hotbarItem.getType())) {
                        updateFuelPost = true;

                        // update fuel if not set or not equal
                        if (getMechanic().getFuel() == null || getMechanic().getFuel().getMaterial() != hotbarItem.getType()) {
                            getMechanic().setFuel(Fuel.getFuel(hotbarItem.getType()));
                        }

                        return false;
                    }
                }

                return true;
            }
        }

        return false;
    }

    private void updateIngredientsPost() {
        if (getMechanic().getIngredientAmount() == 0) {
            getMechanic().setIngredient(null);
            getMechanic().setSmeltResult(null);
        }

        if (getMechanic().getSmeltResult() == null && getMechanic().getIngredient() != null) {
            getMechanic().setSmeltResult(getMechanic().getCachedSmeltResult());
        }
    }

    private void updateIngredients() {
        updateAmount(INGREDIENT_SLOTS, diff -> {
            getMechanic().setIngredientAmount(getMechanic().getIngredientAmount() - diff);
            updateIngredientsPost();
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
            if (!event.isCancelled() && !getMechanic().getTickThrottle().isThrottled()) {
                updateIngredients();
            } else {
                updateIngredientsPost();
            }
        } else if (updateFuelPost) {
            if (!event.isCancelled() && !getMechanic().getTickThrottle().isThrottled()) {
                updateFuel();
            } else {
                if (getMechanic().getFuelAmount() == 0) {
                    getMechanic().setFuel(null);
                }
            }
        }

        updateIngredientsPost = false;
        updateFuelPost = false;
    }
}