package dk.superawesome.factories.mehcanics.impl;

import dk.superawesome.factories.gui.impl.SmelterGui;
import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.mehcanics.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class Smelter extends AbstractMechanic<Smelter, SmelterGui> implements ThinkingMechanic<Smelter, SmelterGui> {

    private final ThinkDelayHandler thinkDelayHandler = new ThinkDelayHandler(20);

    private ItemStack ingredient;
    private int ingredientAmount;

    private ItemStack smeltResult;
    private Fuel fuel;
    private int fuelAmount;
    private Fuel currentFuel;
    private float currentFuelAmount;

    private ItemStack storageType;
    private int storageAmount;

    public Smelter(Location loc) {
        super(loc);
    }

    @Override
    public MechanicProfile<Smelter, SmelterGui> getProfile() {
        return Profiles.SMELTER;
    }

    @Override
    public void pipePut(ItemCollection collection) {
        if (tickThrottle.isThrottled()) {
            return;
        }

        if ((ingredient == null && collection.has(i -> canSmelt(i.getType()))) || ingredient != null && collection.has(ingredient)) {
            ingredientAmount += takeItemsFrom(collection, SmelterGui::updateAddedIngredients, new Storage() {
                @Override
                public ItemStack get() {
                    return ingredient;
                }

                @Override
                public void set(ItemStack stack) {
                    ingredient = stack;
                }
            });
        }

        if ((fuel == null && collection.has(i -> Fuel.get(i.getType()) != null)) || fuel != null && collection.has(new ItemStack(fuel.getMaterial()))) {
            fuelAmount += takeItemsFrom(collection, SmelterGui::updateAddedFuel, new Storage() {
                @Override
                public ItemStack get() {
                    return fuel == null ? null : new ItemStack(fuel.getMaterial());
                }

                @Override
                public void set(ItemStack stack) {
                    fuel = Fuel.get(stack.getType());
                }
            });
        }
    }

    public boolean canSmelt(Material type) {
        if (smeltResult != null && smeltResult.getType() == type) {
            return true;
        }

        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();

            if (recipe instanceof FurnaceRecipe) {
                FurnaceRecipe furnaceRecipe = (FurnaceRecipe) recipe;
                if (furnaceRecipe.getInput().getType() == type) {
                    smeltResult = furnaceRecipe.getResult();
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public ThinkDelayHandler getDelayHandler() {
        return thinkDelayHandler;
    }

    @Override
    public void think() {
        // if there are no ingredients ready to be smelted, don't continue;
        if (ingredient == null || smeltResult == null) {
            return;
        }

        // check if the smelters storage has any previously smelted items which is not the
        // same as the current smelting result.
        // if it has any, we can't smelt the new items until all the previously smelted items are removed
        // from the storage.
        if (storageType != null && !storageType.isSimilar(smeltResult)) {
            return;
        }

        // if there are no fuel left, don't continue
        if (currentFuelAmount == 0 && fuelAmount == 0) {
            return;
        }

        // use fuel
        if (currentFuelAmount > 0) {
            currentFuelAmount -= currentFuel.getFuelAmount();
            if (currentFuelAmount == 0) {
                currentFuel = null;
            }
        }
        if (currentFuelAmount == 0 && fuelAmount > 0) {
            fuelAmount--;
            currentFuelAmount = 1;
            currentFuel = fuel;
            if (fuelAmount == 0) {
                fuel = null;
            }

            SmelterGui gui = inUse.get();
            if (gui != null) {
                gui.updateRemovedFuel(1);
            }
        }

        // update storage type if not set
        if (storageType == null) {
            ItemStack stored = smeltResult;
            stored.setAmount(1);
            storageType = stored;
        }

        // do the smelting
        ingredientAmount -= 1;
        storageAmount += smeltResult.getAmount();

        if (ingredientAmount == 0) {
            ingredient = null;
        }

        SmelterGui gui = inUse.get();
        if (gui != null) {
            gui.updateRemovedIngredients(1);
            gui.updateAddedStorage(smeltResult.getAmount());
            gui.updateFuelState();
        }
    }

    @Override
    public boolean has(ItemStack stack) {
        return has(i -> i.isSimilar(stack) && storageAmount >= stack.getAmount());
    }

    @Override
    public boolean has(Predicate<ItemStack> stack) {
        return storageType != null && stack.test(storageType);
    }

    @Override
    public List<ItemStack> take(int amount) {
        AtomicInteger taken = new AtomicInteger();
        List<ItemStack> items = take(amount, storageType, storageAmount, taken, g -> g.updateRemovedStorage(amount));
        this.storageAmount -= taken.get();

        if (this.storageAmount == 0) {
            this.storageType = null;
        }

        return items;
    }

    public ItemStack getIngredient() {
        return ingredient;
    }

    public void setIngredient(ItemStack stack) {
        this.ingredient = stack;
    }

    public int getIngredientAmount() {
        return ingredientAmount;
    }

    public void setIngredientAmount(int amount) {
        this.ingredientAmount = amount;
    }

    public Fuel getFuel() {
        return fuel;
    }

    public void setFuel(Fuel fuel) {
        this.fuel = fuel;
    }

    public int getFuelAmount() {
        return fuelAmount;
    }

    public void setFuelAmount(int amount) {
        this.fuelAmount = amount;
    }

    public ItemStack getSmeltResult() {
        return smeltResult;
    }

    public void setSmeltResult(ItemStack stack) {
        this.smeltResult = stack;
    }

    public ItemStack getStorageType() {
        return storageType;
    }

    public void setStorageType(ItemStack stack) {
        this.storageType = stack;
    }

    public int getStorageAmount() {
        return storageAmount;
    }

    public void setStorageAmount(int amount) {
        this.storageAmount = amount;
    }

    public float getCurrentFuelAmount() {
        return currentFuelAmount;
    }
}
