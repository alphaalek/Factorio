package dk.superawesome.factories.mehcanics.impl;

import dk.superawesome.factories.gui.impl.SmelterGui;
import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.mehcanics.AbstractMechanic;
import dk.superawesome.factories.mehcanics.Fuel;
import dk.superawesome.factories.mehcanics.MechanicProfile;
import dk.superawesome.factories.mehcanics.Profiles;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class Smelter extends AbstractMechanic<Smelter, SmelterGui> {

    private ItemStack ingredient;
    private int ingredientAmount;
    private Fuel fuel;
    private int fuelAmount;
    private ItemStack storageType;
    private int storageAmount;
    private float currentFuel;

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

    @Override
    public MechanicProfile<Smelter, SmelterGui> getProfile() {
        return Profiles.SMELTER;
    }

    @Override
    public void pipePut(ItemCollection collection) {

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
