package dk.superawesome.factories.gui.impl;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.gui.BaseGui;
import dk.superawesome.factories.gui.MechanicGui;
import dk.superawesome.factories.mehcanics.impl.Constructor;
import dk.superawesome.factories.util.mappings.ItemMappings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.*;

import java.util.*;
import java.util.stream.Collectors;

public class ConstructorGui extends MechanicGui<Constructor> {

    private static final int GRID_WIDTH = 3;
    private static final int GRID_HEIGHT = 3;

    private static final ItemStack EMPTY_CRAFT = new ItemStack(Material.AIR);

    private static final List<Integer> GRAY = Arrays.asList(
            0, 1, 2, 3, 5, 6, 7, 8, 9, 18, 27, 36, 45, 46, 48, 49, 50, 51, 53
    );

    private static final List<Integer> BLACK = Arrays.asList(
            4, 13, 22, 31, 40, 41, 42, 43, 44
    );

    private static final List<Integer> CRAFTING_SLOTS = Arrays.asList(
        10, 11, 12, 19, 20, 21, 28, 29, 30
    );

    private ItemStack craft;

    public ConstructorGui(Constructor constructor) {
        super(constructor, new InitCallbackHolder());
        initCallback.call();

        registerListener(InventoryDragEvent.class, InventoryDragEvent.getHandlerList(), e -> {
            if (e.getInventory().getHolder() == this) {
                e.setCancelled(onDrag(e));
            }
        });

        registerListener(InventoryClickEvent.class, InventoryClickEvent.getHandlerList(), e -> {
            if (e.getClick() == ClickType.DOUBLE_CLICK
                    && e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                Inventory topInv = e.getWhoClicked().getOpenInventory().getTopInventory();
                if (topInv.getHolder() == this) {
                    Bukkit.getScheduler().runTask(Factories.get(), this::updateCrafting);
                }
            }
        });
    }

    @Override
    public void loadItems() {
        super.loadItems();

        for (int i : GRAY) {
            getInventory().setItem(i, BaseGui.GRAY);
        }
        for (int i : BLACK) {
            getInventory().setItem(i, BaseGui.BLACK);
        }
        for (int i = 37; i <= 39; i++) {
            getInventory().setItem(i, ItemMappings.get("crafting_table").generateItem());
        }
        getInventory().setItem(35, new ItemStack(Material.FEATHER, 1));

        getInventory().setItem(47, this.craft = getMechanic().getRecipeResult());
        for (int i = 0; i < 9; i++) {
            getInventory().setItem(CRAFTING_SLOTS.get(i), getMechanic().getCraftingGridItems()[i]);
        }
    }

    @Override
    public void onClose() {
        for (int i = 0; i < 9; i++) {
            getMechanic().getCraftingGridItems()[i] = getInventory().getItem(CRAFTING_SLOTS.get(i));
        }
        getMechanic().setRecipeResult(this.craft);
    }

    @Override
    public boolean onClick(InventoryClickEvent event) {
        if (!CRAFTING_SLOTS.contains(event.getSlot())) {
            return true;
        }

        if (event.getCurrentItem() != null || event.getCursor() != null) {
            Bukkit.getScheduler().runTask(Factories.get(), this::updateCrafting);
        }
        return false;
    }

    public boolean onDrag(InventoryDragEvent event) {
        if (event.getInventorySlots().stream().anyMatch(i -> !CRAFTING_SLOTS.contains(i))) {
            return true;
        }

        Bukkit.getScheduler().runTask(Factories.get(), this::updateCrafting);
        return false;
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
                return initial + Math.min(moveRight, 2) + Math.min(moveDown, 2) * 9;
            }
        }
    }

    private List<ItemStack> getOffer(int from) {
        List<ItemStack> items = new ArrayList<>();
        int diff = from - CRAFTING_SLOTS.get(0);
        for (int i : CRAFTING_SLOTS) {
            int slot = i + diff;
            items.add(CRAFTING_SLOTS.contains(slot) ? getInventory().getItem(slot) : null);
        }

        return items;
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
        // TODO tick limit
        if (getOffer(CRAFTING_SLOTS.get(0)).stream().allMatch(Objects::isNull)) {
            craft = EMPTY_CRAFT;
            getInventory().setItem(47, craft);
            return;
        }

        // iterate over all bukkit recipes and find the recipe matching the one in the crafting grid (if any)
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        boolean found = false;
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next(); // TODO cache crafting recipes

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

                    List<ItemStack> offer = getOffer(getUpperCorner());

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

                        if (at != null) {
                            Iterator<ItemStack> ingredientsIterator = ingredients.iterator();
                            while (ingredientsIterator.hasNext()) {
                                ItemStack ingredient = ingredientsIterator.next();

                                if (ingredient.isSimilar(at) || handleUnspecific(ingredient, at)) {
                                    ingredientsIterator.remove();
                                    offerIterator.remove();
                                    break;
                                }
                            }
                        }
                    }

                    // notify a match success if no required ingredients are left and no more items are inserted into
                    // the grid than what is needed in the recipe
                    if (offer.isEmpty() && ingredients.isEmpty()) {
                        match = true;
                    }
                }

                if (match) {
                    found = true;
                    craft = recipe.getResult();
                    break;
                }
            }
        }

        // we didn't find any recipe, so just place an empty crafting slot
        if (!found) {
            craft = EMPTY_CRAFT;
        }

        // set the crafting slot to the recipe result
        getInventory().setItem(47, craft);
    }
}
