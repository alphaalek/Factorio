package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.util.statics.StringUtil;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.*;

public class RecipeGui<R extends Recipe> extends BaseGuiAdapter<RecipeGui<R>> {

    private final Recipe recipe;

    public RecipeGui(R recipe) {
        super(new InitCallbackHolder(), null, InventoryType.WORKBENCH, "Recipe: " + StringUtil.capitalize(recipe.getResult().getType()), true);
        this.recipe = recipe;
        initCallback.call();
    }

    @Override
    public void loadItems() {
        if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            final List<ItemStack> ingredients = shapelessRecipe.getIngredientList();
            for (int i = 0; i < ingredients.size(); i++) {
                final ItemStack item = ingredients.get(i);
                inventory.setItem(i+1, item);
            }
            inventory.setItem(0, shapelessRecipe.getResult());
        } else if (recipe instanceof ShapedRecipe shapedRecipe) {
            final String[] recipeShape = shapedRecipe.getShape();
            final Map<Character, ItemStack> ingredientMap = shapedRecipe.getIngredientMap();
            for (int j = 0; j < recipeShape.length; j++) {
                for (int k = 0; k < recipeShape[j].length(); k++) {
                    final ItemStack item = ingredientMap.get(recipeShape[j].toCharArray()[k]);
                    if (item == null) {
                        continue;
                    }
                    inventory.setItem(j*3+k+1, item);
                }
            }
            inventory.setItem(0, shapedRecipe.getResult());
        } else {
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i+1, new ItemStack(Material.RED_STAINED_GLASS_PANE));
            }
        }
    }
}
