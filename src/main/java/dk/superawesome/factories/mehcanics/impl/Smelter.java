package dk.superawesome.factories.mehcanics.impl;

import dk.superawesome.factories.gui.impl.SmelterGui;
import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.mehcanics.*;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

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

    @Override
    public MechanicProfile<Smelter, SmelterGui> getProfile() {
        return Profiles.SMELTER;
    }

    @Override
    public void pipePut(ItemCollection collection) {

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
        }
    }

    @Override
    public boolean has(ItemStack stack) {
        return false;
    }

    @Override
    public List<ItemStack> take(int amount) {
        return Collections.EMPTY_LIST;
    }
}
