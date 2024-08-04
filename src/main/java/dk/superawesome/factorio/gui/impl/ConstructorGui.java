package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.impl.Assembler;
import dk.superawesome.factorio.mechanics.impl.Constructor;
import dk.superawesome.factorio.util.helper.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConstructorGui extends MechanicGui<ConstructorGui, Constructor> {

    private static final int GRID_WIDTH = 3;
    private static final int GRID_HEIGHT = 3;
    private static final List<Integer> CRAFTING_SLOTS = Arrays.asList(10, 11, 12, 19, 20, 21, 28, 29, 30);
    private static final List<Integer> STORAGE_SLOTS = Arrays.asList(14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34);

    private static final List<Recipe> commonRecipes = new ArrayList<>();
    private static final List<Tag<Material>> materialTags = new ArrayList<>();

    static {
        for (Assembler.Types type : Assembler.Types.values()) {
            addRecipesFor(new ItemStack(type.getMat()));
        }

        try {
            Class<?> clazz = Class.forName(Tag.class.getName());
            for (Field field : clazz.getDeclaredFields()) {
                Object val = field.get(null);
                if (val instanceof Tag<?> tag) {
                    if (Arrays.stream(((ParameterizedType) tag.getClass().getGenericSuperclass()).getActualTypeArguments())
                            .map(Type::getTypeName)
                            .anyMatch(t -> Material.class.getName().equals(t))) {
                        materialTags.add((Tag<Material>) tag);
                    }
                }
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to acquire material tags", ex);
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
        getInventory().setItem(35, new ItemBuilder(Material.FEATHER).setName("§eOpdatér Inventar").build());

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

        registerEvent(35, __ -> loadInputOutputItems());

        super.loadItems();
    }

    @Override
    public void loadInputOutputItems() {
        if (getMechanic().getStorageType() != null) {
            loadStorageTypes(getMechanic().getStorageType(), getMechanic().getStorageAmount(), STORAGE_SLOTS);
        }
    }

    public void updateDeclinedState(boolean declined) {
        if (declined) {
            for (int i : Arrays.asList(13, 22, 31)) {
                getInventory().setItem(i, new ItemStack(Material.BARRIER));
            }
        } else {
            for (int i : Arrays.asList(13, 22, 31)) {
                getInventory().setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
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
            return (event.getCursor() != null && event.getCursor().getType() != Material.AIR)
                    || event.getAction() == InventoryAction.PLACE_ALL
                    || event.getAction() == InventoryAction.PLACE_SOME
                    || event.getAction() == InventoryAction.PLACE_ONE;
        }

        return true;
    }

    @Override
    public boolean onClickOpen(InventoryClickEvent event) {
        if (movedFromOtherInventory(event)) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    && event.getCurrentItem() != null) {
                if (STORAGE_SLOTS.contains(event.getRawSlot()) && event.getClickedInventory() == getInventory()) {
                    // allow collecting items from storage slots
                    return false;
                } else if (event.getClickedInventory() != getInventory() && !getMechanic().getTickThrottle().isThrottled()) {
                    // ... otherwise put items into crafting slots
                    ItemStack stack = event.getClickedInventory().getItem(event.getSlot());
                    if (stack != null) {
                        addItemsToSlots(stack, CRAFTING_SLOTS);
                    }
                    updateCrafting();

                    return true;
                }
            }

            if (CRAFTING_SLOTS.contains(event.getRawSlot()) && event.getClickedInventory() == getInventory()) {
                getMechanic().getTickThrottle().throttle();
                Bukkit.getScheduler().runTask(Factorio.get(), this::updateCrafting);

                return false;
            }

            if (STORAGE_SLOTS.contains(event.getRawSlot())
                    && (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD)) {
                ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                ItemStack at  = event.getView().getItem(event.getRawSlot());
                if (at != null && hotbarItem != null) {
                    if (hotbarItem.isSimilar(at)) {
                        int add = Math.min(at.getAmount(), hotbarItem.getMaxStackSize() - hotbarItem.getAmount());
                        hotbarItem.setAmount(hotbarItem.getAmount() + add);
                        at.setAmount(at.getAmount() - add);
                    }

                    return true;
                }
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
        return event.getRawSlots().stream().anyMatch(i -> event.getView().getInventory(i).getType() == InventoryType.CHEST);
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

            // get all materials from the tags where the ingredient is tagged
            List<Material> materials = new ArrayList<>();
            for (Tag<Material> tag : materialTags) {
                if (tag.isTagged(mat)) {
                    materials.addAll(tag.getValues());
                }
            }

            // ... get the material occurring the most of the values from these tags
            Material mostOccur = materials.stream()
                    .reduce(BinaryOperator.maxBy(Comparator.comparingInt(o -> Collections.frequency(materials, o))))
                    .orElse(null);
            return mostOccur == offerMat;
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
                    List<ItemStack> ingredients = Arrays.stream(shaped.getShape())
                            .map(r -> new StringBuilder()
                                    .append(r)
                                    .append(new String(new char[3 - r.length()])
                                            .replaceAll("\0", " "))
                            )
                            .flatMap(r -> r.chars().mapToObj(c -> (char) c))
                            .map(c -> shaped.getIngredientMap().get(c))
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
                if (recipe instanceof ShapelessRecipe shapeless) {
                    List<ItemStack> ingredients = shapeless.getIngredientList();
                    List<ItemStack> offer = getOffer(CRAFTING_SLOTS.get(0)).stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()); // can't use Stream#toList because that returns an immutable list

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

    private void searchRecipe() {
        // iterate over all recipes and find the recipe matching the one in the crafting grid (if any)
        searchRecipeIn(commonRecipes.iterator());
        if (this.craft == null) {
            searchRecipeIn(Bukkit.recipeIterator());
        }
    }
}
