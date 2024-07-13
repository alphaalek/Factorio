package dk.superawesome.factories.gui.impl;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.gui.BaseGui;
import dk.superawesome.factories.gui.MechanicGui;
import dk.superawesome.factories.mehcanics.impl.Constructor;
import dk.superawesome.factories.util.mappings.ItemMappings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.*;

import java.util.*;
import java.util.stream.Collectors;

public class ConstructorGui extends MechanicGui<Constructor> {

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
        super(constructor);
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
    }

    private int getUpperCorner() {
        for (int i : CRAFTING_SLOTS) {
            if (getInventory().getItem(i) != null) {
                return i;
            }
        }

        return -1;
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

    private void updateCrafting() {
        if (getOffer(CRAFTING_SLOTS.get(0)).stream().allMatch(Objects::isNull)) {
            craft = EMPTY_CRAFT;
            getInventory().setItem(47, craft);
            return;
        }

        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        boolean found = false;
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next(); // TODO cache crafting recipes

            if (recipe instanceof CraftingRecipe) {

                boolean match = false;

                if (recipe instanceof ShapedRecipe) {
                    ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;

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

                    if (ingredients.stream().allMatch(Objects::isNull)
                            && offer.stream().allMatch(Objects::isNull)) {
                        match = true;
                    }
                }

                if (recipe instanceof ShapelessRecipe) {
                    ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;

                    List<ItemStack> ingredients = shapelessRecipe.getIngredientList();
                    List<ItemStack> offer = getOffer(CRAFTING_SLOTS.get(0)).stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

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

        if (!found) {
            craft = EMPTY_CRAFT;
        }

        getInventory().setItem(47, craft);
    }

    private boolean handleUnspecific(ItemStack ingredient, ItemStack offer) {
        if (ingredient != null && offer != null && ingredient.hasItemMeta()) {
            Material mat = ingredient.getType();
            Material offerMat = offer.getType();

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

    @Override
    public boolean onClick(InventoryClickEvent event) {
        if (!CRAFTING_SLOTS.contains(event.getSlot())) {
            return true;
        }

        Bukkit.getScheduler().runTask(Factories.get(), this::updateCrafting);
        return false;
    }

    @Override
    public boolean onDrag(InventoryDragEvent event) {
        if (event.getInventorySlots().stream().anyMatch(i -> !CRAFTING_SLOTS.contains(i))) {
            return true;
        }

        Bukkit.getScheduler().runTask(Factories.get(), this::updateCrafting);
        return false;
    }
}
