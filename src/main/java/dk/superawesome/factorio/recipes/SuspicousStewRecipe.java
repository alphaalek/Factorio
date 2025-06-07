package dk.superawesome.factorio.recipes;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.*;

public class SuspicousStewRecipe extends ShapelessRecipe {

    private static final List<RecipeChoice> recipeChoices = new ArrayList<>();

    static {
        addRecipeChoice(Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY);
        addRecipeChoice(Material.BROWN_MUSHROOM);
        addRecipeChoice(Material.RED_MUSHROOM);
        addRecipeChoice(Material.BOWL);
    }

    public SuspicousStewRecipe() {
        super(Material.SUSPICIOUS_STEW.getKey(), new ItemStack(Material.SUSPICIOUS_STEW));
    }

    public static void addRecipeChoice(Material... materials) {
        recipeChoices.add(new RecipeChoice.MaterialChoice(materials));
    }

    @Override
    public List<ItemStack> getIngredientList() {
        ArrayList<ItemStack> result = new ArrayList<>(recipeChoices.size());

        for (RecipeChoice ingredient : recipeChoices) {
            result.add(ingredient.getItemStack().clone());
        }

        return result;
    }

    @Override
    public List<RecipeChoice> getChoiceList() {
        List<RecipeChoice> result = new ArrayList<>(recipeChoices.size());

        for (RecipeChoice ingredient : recipeChoices) {
            result.add(ingredient.clone());
        }

        return result;
    }
}
