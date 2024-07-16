package dk.superawesome.factories.mehcanics.impl;

import dk.superawesome.factories.gui.impl.ConstructorGui;
import dk.superawesome.factories.gui.impl.StorageBoxGui;
import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.mehcanics.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Constructor extends AbstractMechanic<Constructor, ConstructorGui> implements ThinkingMechanic<Constructor, ConstructorGui> {

    private final ThinkDelayHandler thinkDelayHandler = new ThinkDelayHandler(20);
    private final ItemStack[] craftingGridItems = new ItemStack[9];
    private ItemStack recipeResult;
    private ItemStack storageType;
    private int storageAmount;

    public Constructor(Location loc) {
        super(loc);
    }

    public ItemStack[] getCraftingGridItems() {
        return craftingGridItems;
    }

    public ItemStack getRecipeResult() {
        return recipeResult;
    }

    public void setRecipeResult(ItemStack result) {
        this.recipeResult = result;
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

    @Override
    public MechanicProfile<Constructor, ConstructorGui> getProfile() {
        return Profiles.CONSTRUCTOR;
    }

    @Override
    public void pipePut(ItemCollection collection) {
        if (tickThrottle.isThrottled()) {
            return;
        }

        for (ItemStack craft : craftingGridItems) {
            // check if this slot contains anything and can hold more
            if (craft == null || craft.getAmount() == craft.getMaxStackSize()) {
                continue;
            }

            // poll the item for this crafting slot from the item collection
            ItemStack req = craft.clone();
            req.setAmount(1);

            if (collection.has(req)) {
                List<ItemStack> stacks = collection.take(req.getAmount());
                if (!stacks.isEmpty() && stacks.get(0).isSimilar(craft)) {
                    craft.setAmount(Math.min(craft.getMaxStackSize(), craft.getAmount() + stacks.get(0).getAmount()));
                }
            }
        }
    }

    @Override
    public ThinkDelayHandler getDelayHandler() {
        return thinkDelayHandler;
    }

    @Override
    public void think() {
        // if there are not any recipe in the crafting grid, don't continue
        if (recipeResult == null) {
            return;
        }

        // check if the constructors storage has any previously crafted items which is not the that are
        // not the same as the current recipe.
        // if it has any, we can't craft the new recipe until all the previously crafted items are removed
        // from the storage.
        if (storageType != null && !storageType.isSimilar(recipeResult)) {
            return;
        }

        // remove one amount from all items in the crafting grid and simulate the crafting
        for (int i = 0; i < 9; i++) {
            ItemStack crafting = craftingGridItems[i];
            if (crafting != null && crafting.getAmount() == 1) {
                // re-set the amounts if the constructor did not have enough items for the recipe
                for (int j = 0; j < i; j++) {
                    ItemStack reSetCrafting = craftingGridItems[i];
                    if (reSetCrafting != null) {
                        reSetCrafting.setAmount(reSetCrafting.getAmount() + 1);
                    }
                }
                return;
            }

            if (crafting != null) {
                crafting.setAmount(crafting.getAmount() - 1);
            }
        }

        // update the storage type if not set
        if (storageType == null) {
            storageType = recipeResult.clone();
            storageType.setAmount(1);
        }

        storageAmount += recipeResult.getAmount();

        ConstructorGui gui = inUse.get();
        if (gui != null) {
            gui.updateAddedItems(recipeResult.getAmount());
        }
    }

    @Override
    public boolean has(ItemStack stack) {
        return storageType != null && storageType.isSimilar(stack);
    }

    @Override
    public List<ItemStack> take(int amount) {
        AtomicInteger taken = new AtomicInteger();
        List<ItemStack> items = take(amount, storageType, storageAmount, taken, g -> g.updateRemovedItems(amount));
        this.storageAmount -= taken.get();
        return items;
    }
}
