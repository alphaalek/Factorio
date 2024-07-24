package dk.superawesome.factories.mechanics.impl;

import dk.superawesome.factories.gui.impl.ConstructorGui;
import dk.superawesome.factories.mechanics.*;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Predicate;

public class Constructor extends AbstractMechanic<Constructor, ConstructorGui> implements ThinkingMechanic, ItemCollection, Container {

    private final ThinkDelayHandler thinkDelayHandler = new ThinkDelayHandler(20);
    private final ItemStack[] craftingGridItems = new ItemStack[9];
    private ItemStack recipeResult;
    private ItemStack storageType;
    private int storageAmount;

    private boolean declinedState;

    public Constructor(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public void load(MechanicStorageContext context) throws Exception {
        ByteArrayInputStream str = context.getData();
        for (int i = 0; i < 9; i++) {
            this.craftingGridItems[i] = context.readItemStack(str);
        }

        this.recipeResult = context.readItemStack(str);
        this.storageType = context.readItemStack(str);
        this.storageAmount = str.read();
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
                    // find an item which has a lower amount than the currently checked item
                    // this is done to ensure evenly distribution among the items in the crafting grid
                    for (ItemStack oCraft : craftingGridItems) {
                        if (oCraft != craft
                                && oCraft != null
                                && oCraft.isSimilar(craft)
                                && oCraft.getAmount() < craft.getAmount()) {
                            craft = oCraft;
                            break;
                        }
                    }

                    craft.setAmount(Math.min(craft.getMaxStackSize(), craft.getAmount() + stacks.get(0).getAmount()));
                }
            }
        }
    }

    @Override
    public int getCapacity() {
        return level.get(ItemCollection.CAPACITY_MARK);
    }

    @Override
    public ThinkDelayHandler getDelayHandler() {
        return thinkDelayHandler;
    }

    @Override
    public void think() {
        // check if the constructors storage has any previously crafted items which is not the that are
        // not the same as the current recipe.
        // if it has any, we can't craft the new recipe until all the previously crafted items are removed
        // from the storage.
        if (storageType != null && recipeResult != null && !storageType.isSimilar(recipeResult)) {
            // set declined state and notify the user that this crafting is not possible yet
            if (!declinedState) {
                declinedState = true;
                ConstructorGui gui = inUse.get();
                if (gui != null) {
                    gui.updateDeclinedState(true);
                }
            }

            return;
        }

        // remove declined state if set and crafting is available
        if (declinedState) {
            declinedState = false;
            ConstructorGui gui = inUse.get();
            if (gui != null) {
                gui.updateDeclinedState(false);
            }
        }

        // if there are not any recipe in the crafting grid, don't continue
        if (recipeResult == null
                // if there is no space left, don't continue
                || storageAmount + recipeResult.getAmount() > getCapacity()) {
            return;
        }

        // remove one amount from all items in the crafting grid and simulate the crafting
        for (int i = 0; i < 9; i++) {
            ItemStack crafting = craftingGridItems[i];
            if (crafting != null && crafting.getAmount() == 1) {
                // re-set the amounts if the constructor did not have enough items for the recipe
                for (int j = 0; j < i; j++) {
                    ItemStack reSetCrafting = craftingGridItems[j];
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
        return has(i -> i.isSimilar(stack) && storageAmount >= stack.getAmount());
    }

    @Override
    public boolean has(Predicate<ItemStack> stack) {
        return storageType != null && stack.test(storageType);
    }

    @Override
    public List<ItemStack> take(int amount) {
        List<ItemStack> items = take(amount, storageType, storageAmount, inUse, g -> g.updateRemovedItems(amount), new Updater<Integer>() {
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
    public double getEnergyCost() {
        return 2d / 3d;
    }

    public boolean isDeclined() {
        return declinedState;
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
}
