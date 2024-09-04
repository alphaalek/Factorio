package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.impl.accessible.Assembler;
import dk.superawesome.factorio.mechanics.impl.accessible.Constructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ConstructorGui extends MechanicGui<ConstructorGui, Constructor> {

    public static final int STORAGE_CONTEXT = 0;

    private static final int GRID_WIDTH = 3;
    private static final int GRID_HEIGHT = 3;
    private static final List<Integer> CRAFTING_SLOTS = Arrays.asList(10, 11, 12, 19, 20, 21, 28, 29, 30);
    public static final List<Integer> STORAGE_SLOTS = Arrays.asList(14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34);//, 35);

    private static final List<Recipe> commonRecipes = new ArrayList<>();

    static {
        for (Assembler.Types type : Assembler.Types.values()) {
            addRecipesFor(new ItemStack(type.getMat()));
        }
    }

    private static void addRecipesFor(ItemStack stack) {
        for (Recipe recipe : Bukkit.getRecipesFor(stack)) {
            if (recipe instanceof CraftingRecipe cr && commonRecipes.stream().map(r -> (CraftingRecipe) r).noneMatch(r -> r.getKey().equals(cr.getKey()))) {
                commonRecipes.add(cr);
                if (recipe instanceof ShapelessRecipe shapeless) {
                    for (ItemStack required : shapeless.getIngredientList()) {
                        addRecipesFor(required);
                    }
                } else if (recipe instanceof ShapedRecipe shaped) {
                    Arrays.stream(shaped.getShape())
                            .flatMap(s -> s.codePoints().mapToObj(c -> (char) c))
                            .map(c -> shaped.getIngredientMap().get(c))
                            .filter(Objects::nonNull)
                            .forEach(ConstructorGui::addRecipesFor);
                }
            }
        }
    }

    private ItemStack craft;

    public ConstructorGui(Constructor constructor, AtomicReference<ConstructorGui> inUseReference) {
        super(constructor, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadItems() {
        for (int i : Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 13, 18, 22, 27, 31, 36, 40, 41, 42, 43, 44, 45, 46, 48, 49, 50)) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        for (int i = 37; i <= 39; i++) {
            getInventory().setItem(i, new ItemStack(Material.CRAFTING_TABLE));
        }

        getInventory().setItem(47, this.craft = getMechanic().getRecipeResult());
        for (int i = 0; i < CRAFTING_SLOTS.size(); i++) {
            getInventory().setItem(CRAFTING_SLOTS.get(i), getMechanic().getCraftingGridItems()[i]);
            // to ensure we also modify the stored crafting grid items as the bukkit stack, we do this hack,
            // which is a seemingly unnecessary inventory operation.

            // this is because CraftInventory#setItem makes a nms copy, so we can't modify the bukkit item
            // and the showed item in the crafting grid at the same time. However, CraftInventory#getItem makes
            // a bukkit mirror, and THEN, we can modify them at the same time!
            getMechanic().getCraftingGridItems()[i] = getInventory().getItem(CRAFTING_SLOTS.get(i));
        }

        if (getMechanic().isDeclined()) {
            updateDeclinedState(true);
        }
        updateCrafting();

        setupHandlePutOrTakeStorageStack(35, getStorage(STORAGE_CONTEXT), STORAGE_SLOTS, false, true);

        super.loadItems();
    }

    @Override
    public void updateItems() {
        if (getMechanic().getStorageType() != null) {
            loadStorageTypes(getMechanic().getStorageType(), getMechanic().getStorageAmount(), STORAGE_SLOTS);
        }
        updateStorageInfo();
    }

    public void updateDeclinedState(boolean declined) {
        if (declined) {
            for (int i : Arrays.asList(13, 22, 31)) {
                getInventory().setItem(i, new ItemStack(Material.BARRIER));
            }
        } else {
            for (int i : Arrays.asList(13, 22, 31)) {
                getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            }
        }
    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        if (getMechanic().getTickThrottle().isThrottled()) {
            return true;
        }

        if (CRAFTING_SLOTS.contains(event.getRawSlot())) {
            if (event.getCurrentItem() != null || event.getCursor() != null) {
                getMechanic().getTickThrottle().throttle();
                Bukkit.getScheduler().runTask(Factorio.get(), this::updateCrafting);
            }

            return false;
        }

        if (STORAGE_SLOTS.contains(event.getRawSlot())) {
            return handleOnlyCollectInteraction(event, getStorage(STORAGE_CONTEXT));
        }

        return true;
    }

    @Override
    public boolean onClickOpen(InventoryClickEvent event) {
        if (movedFromOtherInventory(event)) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && event.getCurrentItem() != null) {
                if (STORAGE_SLOTS.contains(event.getRawSlot())) {
                    // allow collecting items from storage slots, amount update is handled in onClickIn
                    return false;
                } else if (event.getClickedInventory() != getInventory()) {
                    if (getMechanic().getTickThrottle().tryThrottle()) {
                        return true;
                    }

                    // ... otherwise put items into crafting slots
                    ItemStack stack = event.getClickedInventory().getItem(event.getSlot()); // get the literal item
                    if (stack != null) {
                        addItemsToSlots(stack, CRAFTING_SLOTS);
                    }
                    updateCrafting();

                    return true;
                }
            }

            if (CRAFTING_SLOTS.contains(event.getRawSlot()) && event.getClickedInventory() == getInventory()) {
                if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                    if (getMechanic().getTickThrottle().tryThrottle()) {
                        return true;
                    }

                    // update storage amount because this action can collect items from the storage slots
                    updateAmount(getStorage(STORAGE_CONTEXT), event.getWhoClicked().getInventory(), STORAGE_SLOTS, this::updateRemovedItems);
                }

                getMechanic().getTickThrottle().throttle();
                Bukkit.getScheduler().runTask(Factorio.get(), this::updateCrafting);

                return false;
            }

            // check hotbar swap interaction for storage slots
            if (STORAGE_SLOTS.contains(event.getRawSlot())
                    && (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD)
                    && handleOnlyHotbarCollectInteraction(event, getStorage(STORAGE_CONTEXT))) {
                return true;
            }

            // check collect to cursor
            if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                if (getMechanic().getTickThrottle().tryThrottle()) {
                    return true;
                }

                if (event.getClickedInventory() != getInventory()) {
                    // ensure correct gui post action
                    fixSlotsTake(event.getWhoClicked().getInventory(), event.getView(), Stream.concat(CRAFTING_SLOTS.stream(), STORAGE_SLOTS.stream()).toList());
                }

                updateAmount(getStorage(STORAGE_CONTEXT), event.getWhoClicked().getInventory(), STORAGE_SLOTS, this::updateRemovedItems);
                Bukkit.getScheduler().runTask(Factorio.get(), this::updateCrafting);
            }
        }

        return false;
    }

    @Override
    public boolean onDrag(InventoryDragEvent event) {
        if (getMechanic().getTickThrottle().isThrottled()) {
            return true;
        }

        if (CRAFTING_SLOTS.containsAll(event.getRawSlots())) {
            getMechanic().getTickThrottle().throttle();
            Bukkit.getScheduler().runTask(Factorio.get(), this::updateCrafting);
            return false;
        }

        // cancel dragging in constructor gui if it's not in the crafting slots
        return event.getRawSlots().stream().anyMatch(i -> event.getView().getInventory(i) == getInventory());
    }

    private int getUpperCorner() {
        int moveRight = 0;
        int moveDown = 0;

        int initial = CRAFTING_SLOTS.get(0);
        for (;;) {
            int slot = initial + moveRight + moveDown * 9;

            boolean emptyRow = true;
            for (int i = slot; CRAFTING_SLOTS.contains(i); i++) {
                if (getInventory().getItem(i) != null) {
                    emptyRow = false;
                    break;
                }
            }

            boolean emptyColumn = true;
            for (int i = slot; CRAFTING_SLOTS.contains(i); i += 9) {
                if (getInventory().getItem(i) != null) {
                    emptyColumn = false;
                    break;
                }
            }

            if (emptyRow) {
                moveDown++;
            }

            if (emptyColumn) {
                moveRight++;
            }

            if (!emptyRow && !emptyColumn) {
                return slot;
            }

            if (moveRight >= GRID_WIDTH
                    || moveDown >= GRID_HEIGHT) {
                return initial + Math.min(moveRight, GRID_WIDTH - 1) + Math.min(moveDown, GRID_HEIGHT - 1) * 9;
            }
        }
    }

    public void updateAddedItems(int amount) {
        updateAddedItems(getInventory(), amount, getMechanic().getStorageType(), STORAGE_SLOTS);
    }

    public void updateRemovedItems(int amount) {
        updateRemovedItems(getInventory(), amount, getMechanic().getStorageType(),
                IntStream.range(0, STORAGE_SLOTS.size())
                        .boxed()
                        .map(STORAGE_SLOTS::get)
                        .sorted(Collections.reverseOrder())
                        .collect(Collectors.toList()));
    }

    private List<ItemStack> getOffer(int from) {
        List<ItemStack> items = new ArrayList<>();
        int diff = from - CRAFTING_SLOTS.get(0);
        for (int i : CRAFTING_SLOTS) {
            int slot = i + diff;
            items.add(CRAFTING_SLOTS.contains(slot) ? Optional.ofNullable(getInventory().getItem(slot)).map(ItemStack::clone).orElse(null) : null);
        }

        return items;
    }

    private List<ItemStack> getOffer() {
        return getOffer(getUpperCorner());
    }

    private void updateCrafting() {
        for (int i = 0; i < 9; i++) {
            getMechanic().getCraftingGridItems()[i] = getInventory().getItem(CRAFTING_SLOTS.get(i));
        }

        this.craft = null;

        // check if the crafting grid contains any items, if we find any, search for a recipe matching the items
        if (getOffer(CRAFTING_SLOTS.get(0)).stream().anyMatch(Objects::nonNull)) {
            searchRecipe();
        }

        if (this.craft != null && this.craft.getType() == Material.AIR) {
            this.craft = null;
        }

        // set the crafting slot to the recipe result
        getInventory().setItem(47, this.craft);
        getMechanic().setRecipeResult(this.craft);
    }

    private void searchRecipeIn(Iterator<Recipe> recipeIterator) {
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();

            // only check for crafting recipes
            if (recipe instanceof CraftingRecipe) {

                boolean match = false;

                // check for shaped recipe
                // this means the recipe has a specific shape which needs to be inserted into the grid
                if (recipe instanceof ShapedRecipe shaped) {
                    // get the ingredients matrix required for this recipe
                    List<RecipeChoice> choices = Arrays.stream(shaped.getShape())
                            .map(r -> new StringBuilder()
                                    .append(r)
                                    .append(new String(new char[3 - r.length()])
                                            .replaceAll("\0", " "))
                            )
                            .flatMap(r -> r.chars().mapToObj(c -> (char) c))
                            .map(c -> shaped.getChoiceMap().get(c))
                            .collect(Collectors.toList());

                    List<ItemStack> offer = getOffer();

                    // check if the ingredients for the recipe matches the items in the crafting grid
                    for (int i = 0; i < choices.size(); i++) {
                        ItemStack at = offer.get(i);
                        RecipeChoice req = choices.get(i);

                        if (req == null && at != null) {
                            break;
                        }
                        if (req != null && (at == null || !req.test(at))) {
                            break;
                        }

                        offer.set(i, null);
                        choices.set(i, null);
                    }

                    // notify a match success if all items matched
                    if (choices.stream().allMatch(Objects::isNull)
                            && offer.stream().allMatch(Objects::isNull)) {
                        match = true;
                    }
                }

                // check for shapeless recipe
                // this means that the recipe doesn't care what order and position the ingredients are inserted into the grid
                if (recipe instanceof ShapelessRecipe shapeless) {
                    List<RecipeChoice> choices = shapeless.getChoiceList();
                    List<ItemStack> offer = getOffer(CRAFTING_SLOTS.get(0)).stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()); // can't use Stream#toList because that returns an immutable list

                    // loop through all items and check if they match
                    Iterator<ItemStack> offerIterator = offer.iterator();
                    while (offerIterator.hasNext()) {
                        ItemStack at = offerIterator.next();

                        Iterator<RecipeChoice> choiceIterator = choices.iterator();
                        while (choiceIterator.hasNext()) {
                            RecipeChoice choice = choiceIterator.next();

                            if (choice.test(at)) {
                                choiceIterator.remove();
                                offerIterator.remove();
                                break;
                            }
                        }

                        // if either we have checked all the ingredients or items in the crafting grid, we will break the search
                        if (offer.isEmpty() || choices.isEmpty()) {
                            break;
                        }
                    }

                    // notify a match success if no required ingredients are left and no more items are inserted into
                    // the grid than what is needed in the recipe
                    if (offer.isEmpty() && choices.isEmpty()) {
                        match = true;
                    }
                }

                if (match) {
                    this.craft = recipe.getResult();
                    break;
                }
            }
        }
    }

    private void searchRecipe() {
        // iterate over all recipes and find the recipe matching the one in the crafting grid (if any)
        searchRecipeIn(commonRecipes.iterator());
        if (this.craft == null) {
            searchRecipeIn(Bukkit.recipeIterator());
        }
    }
}
