package dk.superawesome.factorio.mechanics.impl;

import dk.superawesome.factorio.gui.impl.SmelterGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.util.BlockVector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class Smelter extends AbstractMechanic<Smelter> implements FuelMechanic, ThinkingMechanic, ItemCollection, ItemContainer {

    public static final int INGREDIENT_STORAGE_CONTEXT = 0;
    public static final int FUEL_STORAGE_CONTEXT = 1;
    public static final int STORED_STORAGE_CONTEXT = 2;

    public static final int INGREDIENT_CAPACITY = 1;
    private static final List<BlockVector> WASTE_OUTPUT_RELATIVES = Arrays.asList(
            new BlockVector(0, 2, 0),
            new BlockVector(0, 1, 1),
            new BlockVector(0, 1, -1),
            new BlockVector(1, 1, 0),
            new BlockVector(-1, 1, 0)
    );

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

        loadFuel(context, str);

        this.storageType = context.getSerializer().readItemStack(str);
        this.storageAmount = context.getSerializer().readInt(str);
    }

    @Override
    public void save(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        context.getSerializer().writeItemStack(str, this.ingredient);
        context.getSerializer().writeInt(str, this.ingredientAmount);
        context.getSerializer().writeItemStack(str, this.smeltResult);

        saveFuel(context, str);

        context.getSerializer().writeItemStack(str, this.storageType);
        context.getSerializer().writeInt(str, this.storageAmount);

        context.uploadData(str);
    }

    @Override
    public MechanicProfile<Smelter> getProfile() {
        return Profiles.SMELTER;
    }

    @Override
    public void pipePut(ItemCollection collection, PipePutEvent event) {
        if (tickThrottle.isThrottled()) {
            return;
        }

        if (ingredient != null && collection.has(ingredient) || ingredient == null && collection.has(i -> canSmelt(i.getType()))) {
            ingredientAmount += this.<SmelterGui>put(collection, level.getInt(INGREDIENT_CAPACITY) - ingredientAmount, getGuiInUse(), SmelterGui::updateAddedIngredients, new HeapToStackAccess<>() {
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

        this.<SmelterGui>putFuel(collection, this, event, getGuiInUse(), SmelterGui::updateAddedFuel);
    }

    @Override
    public int getCapacity() {
        return level.get(ItemCollection.CAPACITY_MARK);
    }

    @Override
    public int getFuelCapacity() {
        return level.getInt(FUEL_CAPACITY);
    }

    @Override
    public List<BlockVector> getWasteOutputs() {
        return WASTE_OUTPUT_RELATIVES;
    }

    public boolean canSmelt(Material type) {
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();

            if (recipe instanceof FurnaceRecipe furnaceRecipe) {
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
                SmelterGui gui = this.<SmelterGui>getGuiInUse().get();
                if (gui != null) {
                    gui.updateDeclinedState(true);
                }
            }

            return;
        }

        // remove declined state if set and smelting is available
        if (declinedState) {
            declinedState = false;
            SmelterGui gui = this.<SmelterGui>getGuiInUse().get();
            if (gui != null) {
                gui.updateDeclinedState(false);
            }
        }

        // if there are no ingredients ready to be smelted, don't continue
        if (ingredient == null || smeltResult == null
                // if there is no space left, don't continue
                || storageAmount + smeltResult.getAmount() > getCapacity()) {
            return;
        }

        FuelState state = useFuel();
        if (state == FuelState.ABORT) {
            return;
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

        SmelterGui gui = this.<SmelterGui>getGuiInUse().get();
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

        return this.<SmelterGui>take(Math.min(storageType.getMaxStackSize(), amount), storageType, storageAmount, getGuiInUse(), SmelterGui::updateRemovedStorage, new HeapToStackAccess<>() {
            @Override
            public Integer get() {
                return storageAmount;
            }

            @Override
            public void set(Integer val) {
                setStorageAmount(storageAmount - val);
            }
        });
    }

    @Override
    public boolean isTransferEmpty() {
        return storageType == null;
    }

    @Override
    public int getMaxTransfer() {
        return storageType.getMaxStackSize();
    }

    @Override
    public int getTransferAmount() {
        return storageAmount;
    }

    @Override
    public boolean isContainerEmpty() {
        return fuel == null && ingredient == null;
    }

    @Override
    public double getTransferEnergyCost() {
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

        if (this.ingredientAmount == 0) {
            ingredient = null;
            cachedSmeltResult = null;
        }
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

        if (this.fuelAmount == 0) {
            fuel = null;
        }
    }

    @Override
    public Fuel getCurrentFuel() {
        return currentFuel;
    }

    @Override
    public void setCurrentFuel(Fuel fuel) {
        this.currentFuel = fuel;
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

        if (this.storageAmount == 0) {
            storageType = null;
        }
    }

    public float getCurrentFuelAmount() {
        return currentFuelAmount;
    }

    @Override
    public void setCurrentFuelAmount(float amount) {
        this.currentFuelAmount = amount;
    }

    @Override
    public void removeFuel(int amount) {
        SmelterGui gui = this.<SmelterGui>getGuiInUse().get();
        if (gui != null) {
            gui.updateRemovedFuel(amount);
        }
    }

    public boolean isDeclined() {
        return declinedState;
    }

    public ItemStack getCachedSmeltResult() {
        return cachedSmeltResult;
    }
}
