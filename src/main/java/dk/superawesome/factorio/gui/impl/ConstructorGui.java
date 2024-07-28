package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.impl.Constructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConstructorGui extends MechanicGui<ConstructorGui, Constructor> {

    private static final int GRID_WIDTH = 3;
    private static final int GRID_HEIGHT = 3;
    private static final List<Integer> GRAY = Arrays.asList(0, 1, 2, 3, 5, 6, 7, 8, 9, 18, 27, 36, 45, 46, 48, 49, 50, 51, 53);
    private static final List<Integer> BLACK = Arrays.asList(4, 13, 22, 31, 40, 41, 42, 43, 44);
    private static final List<Integer> DECLINE = Arrays.asList(13, 22, 31);
    private static final List<Integer> CRAFTING_SLOTS = Arrays.asList(10, 11, 12, 19, 20, 21, 28, 29, 30);
    private static final List<Integer> STORAGE_SLOTS = Arrays.asList(14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34);

    private ItemStack craft;

    public ConstructorGui(Constructor constructor, AtomicReference<ConstructorGui> inUseReference) {
        super(constructor, inUseReference, new InitCallbackHolder());
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
        for (int i = 37; i <= 39; i++) {
            getInventory().setItem(i, new ItemStack(Material.CRAFTING_TABLE));
        }
        getInventory().setItem(35, new ItemStack(Material.FEATHER, 1));

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
    }

    @Override
    public void loadInputOutputItems() {
        if (getMechanic().getStorageType() != null) {
            loadStorageTypes(getMechanic().getStorageType(), getMechanic().getStorageAmount(), STORAGE_SLOTS);
        }
    }

    @Override
    public void onClose() {

    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        if (getMechanic().getTickThrottle().isThrottled()) {
            return true;
        }

        if (!CRAFTING_SLOTS.contains(event.getRawSlot())) {
            if (event.getRawSlot() == 35) {
                loadInputOutputItems();
            }

            return true;
        }

        if (event.getCurrentItem() != null || event.getCursor() != null) {
            getMechanic().getTickThrottle().throttle();
            Bukkit.getScheduler().runTask(Factorio.get(), this::updateCrafting);
        }

        return false;
    }

    @Override
    public boolean onClickOpen(InventoryClickEvent event) {
        if (movedFromOtherInventory(event)) {

            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    && event.getClickedInventory() != null
                    && event.getClickedInventory() != getInventory()
                    && event.getCurrentItem() != null) {
                ItemStack stack = event.getClickedInventory().getItem(event.getSlot());
                if (stack != null) {
                    addItemsToSlots(stack, CRAFTING_SLOTS);
                }

                return true;
            }

            if (!CRAFTING_SLOTS.contains(event.getRawSlot())
                    && event.getClickedInventory() == getInventory()) {
                return true;
            }

            getMechanic().getTickThrottle().throttle();
            Bukkit.getScheduler().runTask(Factorio.get(), this::updateCrafting);
        }

        return false;
    }

    @Override
    public void onClickPost(InventoryClickEvent event) {

    }

    @Override
    public boolean onDrag(InventoryDragEvent event) {
        if (getMechanic().getTickThrottle().isThrottled()) {
            return true;
        }

        if (event.getRawSlots().stream().anyMatch(i -> !CRAFTING_SLOTS.contains(i))) {
            return true;
        }

        getMechanic().getTickThrottle().throttle();
        Bukkit.getScheduler().runTask(Factorio.get(), this::updateCrafting);
        return false;
    }

    public void updateDeclinedState(boolean declined) {
        if (declined) {
            for (int i : DECLINE) {
                getInventory().setItem(i, new ItemStack(Material.BARRIER));
            }
        } else {
            for (int i : DECLINE) {
                getInventory().setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
            }
        }
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

    private boolean handleUnspecific(ItemStack ingredient, ItemStack offer) {
        if (ingredient != null && offer != null && ingredient.hasItemMeta()) {
            Material mat = ingredient.getType();
            Material offerMat = offer.getType();

            // check for similar item variants
            if (Tag.BASE_STONE_NETHER.isTagged(mat)
                    && Tag.BASE_STONE_NETHER.isTagged(offerMat)) {
                return true;
            } else if (Tag.BASE_STONE_OVERWORLD.isTagged(mat)
                    && Tag.BASE_STONE_OVERWORLD.isTagged(offerMat)) {
                return true;
            } else if (Tag.PLANKS.isTagged(mat)
                    && Tag.PLANKS.isTagged(offerMat)) {
                return true;
            }
        }

        return false;
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

        // set the crafting slot to the recipe result
        getInventory().setItem(47, this.craft);
        getMechanic().setRecipeResult(this.craft);
    }

    private void searchRecipe() {
        // TODO tick limit

        // iterate over all bukkit recipes and find the recipe matching the one in the crafting grid (if any)
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next(); // TODO cache common recipes

            // only check for crafting recipes
            if (recipe instanceof CraftingRecipe) {

                boolean match = false;

                // check for shaped recipe
                // this means the recipe has a specific shape which needs to be inserted into the grid
                if (recipe instanceof ShapedRecipe) {
                    ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;

                    // get the ingredients matrix required for this recipe
                    List<ItemStack> ingredients = Arrays.stream(shapedRecipe.getShape())
                            .map(r -> new StringBuilder()
                                    .append(r)
                                    .append(new String(new char[3 - r.length()])
                                            .replaceAll("\0", " "))
                            )
                            .flatMap(r -> r.chars().mapToObj(c -> (char) c))
                            .map(c -> shapedRecipe.getIngredientMap().get(c))
                            .collect(Collectors.toList());

                    List<ItemStack> offer = getOffer();

                    // check if the ingredients for the recipe matches the items in the crafting grid
                    for (int i = 0; i < ingredients.size(); i++) {
                        ItemStack at = offer.get(i);
                        ItemStack req = ingredients.get(i);

                        if (req == null && at != null) {
                            break;
                        }
                        if (req != null && !req.isSimilar(at) && !handleUnspecific(req, at)) {
                            break;
                        }

                        offer.set(i, null);
                        ingredients.set(i, null);
                    }

                    // notify a match success if all items matched
                    if (ingredients.stream().allMatch(Objects::isNull)
                            && offer.stream().allMatch(Objects::isNull)) {
                        match = true;
                    }
                }

                // check for shapeless recipe
                // this means that the recipe doesn't care what order and position the ingredients are inserted into the grid
                if (recipe instanceof ShapelessRecipe) {
                    ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;

                    List<ItemStack> ingredients = shapelessRecipe.getIngredientList();
                    List<ItemStack> offer = getOffer(CRAFTING_SLOTS.get(0)).stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    // loop through all items and check if they match
                    Iterator<ItemStack> offerIterator = offer.iterator();
                    while (offerIterator.hasNext()) {
                        ItemStack at = offerIterator.next();

                        Iterator<ItemStack> ingredientsIterator = ingredients.iterator();
                        while (ingredientsIterator.hasNext()) {
                            ItemStack ingredient = ingredientsIterator.next();

                            if (ingredient.isSimilar(at) || handleUnspecific(ingredient, at)) {
                                ingredientsIterator.remove();
                                offerIterator.remove();
                                break;
                            }
                        }

                        // if either we have checked all the ingredients or items in the crafting grid, we will break the search
                        if (offer.isEmpty() || ingredients.isEmpty()) {
                            break;
                        }
                    }

                    // notify a match success if no required ingredients are left and no more items are inserted into
                    // the grid than what is needed in the recipe
                    if (offer.isEmpty() && ingredients.isEmpty()) {
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
}