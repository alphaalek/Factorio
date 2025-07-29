package dk.superawesome.factorio.mechanics.impl.accessible;

import dk.superawesome.factorio.gui.impl.SmelterGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.stackregistry.Fuel;
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
import java.util.*;
import java.util.function.Predicate;

public class Smelter extends AbstractMechanic<Smelter> implements FuelMechanic, AccessibleMechanic, ThinkingMechanic, ItemCollection, ItemContainer {

    public static final int INGREDIENT_CAPACITY_MARK = 1;
    public static final int FUEL_CAPACITY_MARK = 2;

    private static final List<BlockVector> WASTE_OUTPUT_RELATIVES = Arrays.asList(
            new BlockVector(0, 2, 0),

            new BlockVector(0, 1, 1),
            new BlockVector(0, 1, -1),
            new BlockVector(1, 1, 0),
            new BlockVector(-1, 1, 0),

            new BlockVector(0, 0, 1),
            new BlockVector(0, 0, -1),
            new BlockVector(1, 0, 0),
            new BlockVector(-1, 0, 0)
    );

    private static final List<FurnaceRecipe> FURNACE_RECIPES = new ArrayList<>();

    static {
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();

            if (recipe instanceof FurnaceRecipe furnaceRecipe) {
                FURNACE_RECIPES.add(furnaceRecipe);
            }
        }
    }

    private final Storage ingredientStorage = getProfile().getStorageProvider().createStorage(this, SmelterGui.INGREDIENT_CONTEXT);
    private final Storage storedStorage = getProfile().getStorageProvider().createStorage(this, SmelterGui.STORED_CONTEXT);
    private final XPDist xpDist = new XPDist(100, 0.001, 0.01);
    private final DelayHandler thinkDelayHandler = new DelayHandler(level.get(MechanicLevel.THINK_DELAY_MARK));
    private final DelayHandler transferDelayHandler = new DelayHandler(10);

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

    public Smelter(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public void loadData(ByteArrayInputStream data) throws Exception {
        this.ingredient = this.context.getSerializer().readItemStack(data);
        this.smeltResult = this.context.getSerializer().readItemStack(data);
        setIngredientAmount(this.context.getSerializer().readInt(data)); // ensure no zero if ingredient set

        loadFuel(this.context, data);

        this.storageType = this.context.getSerializer().readItemStack(data);
        setStorageAmount(this.context.getSerializer().readInt(data)); // ensure no zero if storage set

        clearSmeltResult: {
            if (this.ingredientAmount > 0 && this.ingredient == null) {
                this.ingredientAmount = 0;
            } else if (this.ingredientAmount == 0 && this.ingredient != null) {
                this.ingredient = null;
            } else break clearSmeltResult;

            this.smeltResult = null;
            this.cachedSmeltResult = null;
        }
        if (this.fuelAmount > 0 && this.fuel == null) {
            this.fuelAmount = 0;
        } else if (this.fuelAmount == 0 && this.fuel != null) {
            this.fuel = null;
        }
        if (this.storageAmount > 0 && this.storageType == null) {
            this.storageAmount = 0;
        } else if (this.storageAmount == 0 && this.storageType != null) {
            this.storageType = null;
        }
    }

    @Override
    public Optional<ByteArrayOutputStream> saveData() throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        this.context.getSerializer().writeItemStack(data, this.ingredient);
        this.context.getSerializer().writeItemStack(data, this.smeltResult);
        this.context.getSerializer().writeInt(data, this.ingredientAmount);

        saveFuel(this.context, data);

        this.context.getSerializer().writeItemStack(data, this.storageType);
        this.context.getSerializer().writeInt(data, this.storageAmount);

        return Optional.of(data);
    }

    @Override
    public void onUpgrade(int newLevel) {
        this.thinkDelayHandler.setDelay(this.level.getInt(MechanicLevel.THINK_DELAY_MARK));

        super.onUpgrade(newLevel);
    }

    @Override
    public MechanicProfile<Smelter> getProfile() {
        return Profiles.SMELTER;
    }

    @Override
    public void pipePut(ItemCollection collection, PipePutEvent event) {
        this.ingredientStorage.ensureValidStorage();

        if (this.tickThrottle.isThrottled()) {
            return;
        }

        if (this.ingredient != null && collection.has(this.ingredient) || this.ingredient == null && collection.has(this::canSmelt)) {
            int add = this.<SmelterGui>put(collection, getIngredientCapacity() - this.ingredientAmount, getGuiInUse(), SmelterGui::updateAddedIngredients, ingredientStorage);
            if (add > 0) {
                this.ingredientAmount += add;
                event.setTransferred(true);
            }

            if (this.smeltResult == null) {
                this.smeltResult = this.cachedSmeltResult;
            }
        }

        this.<SmelterGui>putFuel(collection, this, event, getGuiInUse(), SmelterGui::updateAddedFuel);
    }

    @Override
    public int getCapacity() {
        return getCapacitySlots(this.level) *
                Optional.ofNullable(this.storageType)
                        .map(ItemStack::getMaxStackSize)
                        .orElse(64);
    }

    @Override
    public int getFuelCapacity() {
        return this.level.getInt(FUEL_CAPACITY_MARK) *
                Optional.ofNullable(this.fuel)
                        .map(Fuel::material)
                        .map(Material::getMaxStackSize)
                        .orElse(64);
    }

    public int getIngredientCapacity() {
        return this.level.getInt(INGREDIENT_CAPACITY_MARK) *
                Optional.ofNullable(this.ingredient)
                        .map(ItemStack::getMaxStackSize)
                        .orElse(64);
    }

    @Override
    public List<BlockVector> getWasteOutputs() {
        return WASTE_OUTPUT_RELATIVES;
    }

    public static boolean canSmeltStatic(ItemStack stack) {
        return FURNACE_RECIPES.stream().anyMatch(r -> r.getInputChoice().test(stack));
    }

    public boolean canSmelt(ItemStack stack) {
        boolean can = FURNACE_RECIPES.stream().peek(r -> this.cachedSmeltResult = r.getResult()).anyMatch(r -> r.getInputChoice().test(stack));

        if (!can) {
            this.cachedSmeltResult = null;
        }

        return can;
    }

    @Override
    public DelayHandler getThinkDelayHandler() {
        return thinkDelayHandler;
    }

    @Override
    public void think() {
        this.cachedSmeltResult = null;

        // check if the smelters storage has any previously smelted items which is not the
        // same as the current smelting result.
        // if it has any, we can't smelt the new items until all the previously smelted items are removed
        // from the storage.
        if (this.storageType != null && this.smeltResult != null && !this.storageType.isSimilar(this.smeltResult)) {
            // set declined state and notify the user that this smelting is not possible yet
            if (!this.declinedState) {
                this.declinedState = true;
                SmelterGui gui = this.<SmelterGui>getGuiInUse().get();
                if (gui != null) {
                    gui.updateDeclinedState(true);
                }
            }

            return;
        }

        // remove declined state if set and smelting is available
        if (this.declinedState) {
            this.declinedState = false;
            SmelterGui gui = this.<SmelterGui>getGuiInUse().get();
            if (gui != null) {
                gui.updateDeclinedState(false);
            }
        }

        // update smelt result if it failed to do so
        if (this.ingredient != null && this.smeltResult == null) {
            canSmelt(this.ingredient);
            this.smeltResult = this.cachedSmeltResult;

            if (this.smeltResult == null) {
                // this item can not be smelted, it shouldn't be in the smelter
                this.ingredient = null;
                this.ingredientAmount = 0;
                return;
            }
        }

        // if there are no ingredients ready to be smelted, don't continue
        if (this.ingredient == null
                // if there is no space left, don't continue
                || this.storageAmount + this.smeltResult.getAmount() > getCapacity()) {
            return;
        }

        FuelState state = useFuel();
        if (state == FuelState.ABORT) {
            return;
        }

        // update storage type if not set
        if (this.storageType == null) {
            ItemStack stored = smeltResult;
            stored.setAmount(1);
            storageType = stored;
        }

        // do the smelting
        this.ingredientAmount -= 1;
        this.storageAmount += this.smeltResult.getAmount();

        this.xp += this.xpDist.poll();

        SmelterGui gui = this.<SmelterGui>getGuiInUse().get();
        if (gui != null) {
            gui.updateRemovedIngredients(1);
            gui.updateAddedStorage(this.smeltResult.getAmount());
            gui.updateFuelState();
        }

        // the smelter does not have any ingredients left, clear up
        if (this.ingredientAmount == 0) {
            this.ingredient = null;
            this.cachedSmeltResult = null;
            this.smeltResult = null;
        }
    }

    @Override
    public boolean has(ItemStack stack) {
        return has(i -> i.isSimilar(stack) && this.storageAmount >= stack.getAmount());
    }

    @Override
    public boolean has(Predicate<ItemStack> stack) {
        return this.storageType != null && stack.test(this.storageType);
    }

    @Override
    public List<ItemStack> pipeTake(int amount) {
        this.storedStorage.ensureValidStorage();

        if (this.tickThrottle.isThrottled() || this.storageType == null || this.storageAmount == 0) {
            return Collections.emptyList();
        }

        return this.<SmelterGui>take((int) Math.min(getMaxTransfer(), amount), this.storageType, this.storageAmount, getGuiInUse(), SmelterGui::updateRemovedStorage, this.storedStorage);
    }

    @Override
    public boolean isTransferEmpty() {
        return this.storageType == null;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return this.transferDelayHandler;
    }

    @Override
    public double getMaxTransfer() {
        return this.storageType.getMaxStackSize();
    }

    @Override
    public double getTransferAmount() {
        return this.storageAmount;
    }

    @Override
    public boolean isContainerEmpty() {
        return this.fuel == null && this.ingredient == null;
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d / 2d;
    }

    public ItemStack getIngredient() {
        return this.ingredient;
    }

    public void setIngredient(ItemStack stack) {
        this.ingredient = stack;
    }

    public int getIngredientAmount() {
        return this.ingredientAmount;
    }

    public void setIngredientAmount(int amount) {
        this.ingredientAmount = amount;

        if (this.ingredientAmount == 0) {
            this.ingredient = null;
            this.cachedSmeltResult = null;
            this.smeltResult = null;
        }
    }

    @Override
    public Fuel getFuel() {
        return this.fuel;
    }

    @Override
    public void setFuel(Fuel fuel) {
        this.fuel = fuel;

        if (this.fuel == null) {
            this.fuelAmount = 0;
        }
    }

    @Override
    public int getFuelAmount() {
        return this.fuelAmount;
    }

    @Override
    public void setFuelAmount(int amount) {
        this.fuelAmount = amount;

        if (this.fuelAmount == 0) {
            this.fuel = null;
        }
    }

    @Override
    public Fuel getCurrentFuel() {
        return this.currentFuel;
    }

    @Override
    public void setCurrentFuel(Fuel fuel) {
        this.currentFuel = fuel;

        if (this.currentFuel == null) {
            this.currentFuelAmount = 0;
        }
    }

    public ItemStack getSmeltResult() {
        return this.smeltResult;
    }

    public void setSmeltResult(ItemStack stack) {
        this.smeltResult = stack;
    }

    public ItemStack getStorageType() {
        return this.storageType;
    }

    public void setStorageType(ItemStack stack) {
        this.storageType = stack;

        if (this.storageType == null) {
            this.storageAmount = 0;
        }
    }

    public int getStorageAmount() {
        return this.storageAmount;
    }

    public void setStorageAmount(int amount) {
        this.storageAmount = amount;

        if (this.storageAmount == 0) {
            this.storageType = null;
        }
    }

    @Override
    public float getCurrentFuelAmount() {
        return this.currentFuelAmount;
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
        return this.declinedState;
    }

    public ItemStack getCachedSmeltResult() {
        return this.cachedSmeltResult;
    }
}
