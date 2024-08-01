package dk.superawesome.factorio.mechanics.impl;

import dk.superawesome.factorio.gui.impl.SmelterGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.items.Container;
import dk.superawesome.factorio.mechanics.items.Fuel;
import dk.superawesome.factorio.mechanics.items.ItemCollection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class Smelter extends AbstractMechanic<Smelter, SmelterGui> implements ThinkingMechanic, ItemCollection, Container {

    public static final int INGREDIENT_CAPACITY = 1;
    public static final int FUEL_CAPACITY = 2;

    private final ThinkDelayHandler thinkDelayHandler = new ThinkDelayHandler(20);

    private ItemStack ingredient;
    private int ingredientAmount;

    private ItemStack cachedSmeltResult;
    private ItemStack smeltResult;
    private Fuel fuel;
    private int fuelAmount;
    private Fuel currentFuel;
    private float currentFuelAmount;

    private boolean declinedState;
    private ItemStack storageType;
    private int storageAmount;

    public Smelter(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
        loadFromStorage();
    }

    @Override
    public void load(MechanicStorageContext context) throws Exception {
        ByteArrayInputStream str = context.getData();
        this.ingredient = context.getSerializer().readItemStack(str);
        this.ingredientAmount = context.getSerializer().readInt(str);
        this.smeltResult = context.getSerializer().readItemStack(str);

        ItemStack fuel = context.getSerializer().readItemStack(str);
        if (fuel != null) {
            this.fuel = Fuel.getFuel(fuel.getType());
        }
        this.fuelAmount = context.getSerializer().readInt(str);
        ItemStack currentFuel = context.getSerializer().readItemStack(str);
        int currentFuelAmount = context.getSerializer().readInt(str);
        if (currentFuel != null) {
            this.currentFuel = Fuel.getFuel(currentFuel.getType());
            this.currentFuelAmount = 1 - this.currentFuel.getFuelAmount() * currentFuelAmount;
        }

        this.storageType = context.getSerializer().readItemStack(str);
        this.storageAmount = context.getSerializer().readInt(str);
    }

    @Override
    public void save(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        context.getSerializer().writeItemStack(str, this.ingredient);
        context.getSerializer().writeInt(str, this.ingredientAmount);
        context.getSerializer().writeItemStack(str, this.smeltResult);

        if (this.fuel != null) {
            context.getSerializer().writeItemStack(str, new ItemStack(this.fuel.getMaterial()));
        } else {
            context.getSerializer().writeItemStack(str, null);
        }
        context.getSerializer().writeInt(str, this.fuelAmount);
        if (this.currentFuel != null) {
            context.getSerializer().writeItemStack(str, new ItemStack(this.currentFuel.getMaterial()));
            context.getSerializer().writeInt(str, (int) ((1 - this.currentFuelAmount) / this.currentFuel.getFuelAmount()));
        } else {
            context.getSerializer().writeItemStack(str, null);
            context.getSerializer().writeInt(str, 0);
        }

        context.getSerializer().writeItemStack(str, this.storageType);
        context.getSerializer().writeInt(str, this.storageAmount);

        context.uploadData(str);
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

        if (ingredient != null && collection.has(ingredient) || ingredient == null && collection.has(i -> canSmelt(i.getType()))) {
            ingredientAmount += put(collection, Math.min(64, level.getInt(INGREDIENT_CAPACITY) - ingredientAmount), inUse, SmelterGui::updateAddedIngredients, new Updater<ItemStack>() {
                @Override
                public ItemStack get() {
                    return ingredient;
                }

                @Override
                public void set(ItemStack stack) {
                    ingredient = stack;
                }
            });

            if (smeltResult == null) {
                smeltResult = cachedSmeltResult;
            }
        }

        if (fuel != null && collection.has(new ItemStack(fuel.getMaterial())) || fuel == null && collection.has(i -> Fuel.getFuel(i.getType()) != null)) {
            fuelAmount += put(collection, Math.min(64, level.getInt(FUEL_CAPACITY) - fuelAmount), inUse, SmelterGui::updateAddedFuel, new Updater<ItemStack>() {
                @Override
                public ItemStack get() {
                    return fuel == null ? null : new ItemStack(fuel.getMaterial());
                }

                @Override
                public void set(ItemStack stack) {
                    fuel = Fuel.getFuel(stack.getType());
                }
            });
        }
    }

    @Override
    public int getCapacity() {
        return level.get(ItemCollection.CAPACITY_MARK);
    }

    public boolean canSmelt(Material type) {
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();

            if (recipe instanceof FurnaceRecipe) {
                FurnaceRecipe furnaceRecipe = (FurnaceRecipe) recipe;
                if (furnaceRecipe.getInput().getType() == type) {
                    cachedSmeltResult = furnaceRecipe.getResult();
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
        // check if the smelters storage has any previously smelted items which is not the
        // same as the current smelting result.
        // if it has any, we can't smelt the new items until all the previously smelted items are removed
        // from the storage.
        if (storageType != null && smeltResult != null && !storageType.isSimilar(smeltResult)) {
            // set declined state and notify the user that this smelting is not possible yet
            if (!declinedState) {
                declinedState = true;
                SmelterGui gui = inUse.get();
                if (gui != null) {
                    gui.updateDeclinedState(true);
                }
            }

            return;
        }

        // remove declined state if set and smelting is available
        if (declinedState) {
            declinedState = false;
            SmelterGui gui = inUse.get();
            if (gui != null) {
                gui.updateDeclinedState(false);
            }
        }

        // if there are no ingredients ready to be smelted, don't continue
        if (ingredient == null || smeltResult == null
                // if there are no fuel left, don't continue
                || (currentFuelAmount == 0 && fuelAmount == 0)
                // if there is no space left, don't continue
                || storageAmount + smeltResult.getAmount() > getCapacity()) {
            return;
        }

        // use fuel
        if (currentFuelAmount == 0 && fuelAmount > 0) {
            SmelterGui gui = inUse.get();
            if (gui != null) {
                gui.updateRemovedFuel(1);
            }

            // remove the fuel internally after we updated to gui
            fuelAmount--;
            currentFuelAmount = 1;
            currentFuel = fuel;
            if (fuelAmount <= 0) {
                fuel = null;
            }
        }
        if (currentFuelAmount > 0) {
            currentFuelAmount -= currentFuel.getFuelAmount();
            // due to working with floats, there can be calculation errors due to java binary encoding
            // this means that we can possibly end up with a number slightly above zero
            if (currentFuelAmount <= .001) {
                currentFuel = null;
                currentFuelAmount = 0; // ensure zero value (related problem mentioned above)
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

        SmelterGui gui = inUse.get();
        if (gui != null) {
            gui.updateRemovedIngredients(1);
            gui.updateAddedStorage(smeltResult.getAmount());
            gui.updateFuelState();
        }

        // the smelter does not have any ingredients left, clear up
        if (ingredientAmount == 0) {
            ingredient = null;
            smeltResult = null;
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
        List<ItemStack> items = take(amount, storageType, storageAmount, inUse, g -> g.updateRemovedStorage(amount), new Updater<Integer>() {
            @Override
            public Integer get() {
                return storageAmount;
            }

            @Override
            public void set(Integer val) {
                storageAmount -= val;
            }
        });

        if (this.storageAmount == 0) {
            this.storageType = null;
        }

        return items;
    }

    @Override
    public boolean isEmpty() {
        return storageType == null;
    }

    @Override
    public boolean isContainerEmpty() {
        return fuel == null && ingredient == null;
    }

    @Override
    public double getEnergyCost() {
        return 1d / 2d;
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

    public boolean isDeclined() {
        return declinedState;
    }

    public ItemStack getCachedSmeltResult() {
        return cachedSmeltResult;
    }
}
