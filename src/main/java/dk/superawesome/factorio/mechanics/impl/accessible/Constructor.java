package dk.superawesome.factorio.mechanics.impl.accessible;

import dk.superawesome.factorio.gui.SingleStorageGui;
import dk.superawesome.factorio.gui.impl.ConstructorGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;

public class Constructor extends AbstractMechanic<Constructor> implements AccessibleMechanic, ThinkingMechanic, ItemCollection, ItemContainer {

    public static final int UNIT_TRANSFER_AMOUNT_MARK = 1;

    private final Storage storage = getProfile().getStorageProvider().createStorage(this, SingleStorageGui.CONTEXT);
    private final XPDist xpDist = new XPDist(100, 0.0025, 0.01);
    private final DelayHandler thinkDelayHandler = new DelayHandler(level.get(MechanicLevel.THINK_DELAY_MARK));
    private final DelayHandler transferDelayHandler = new DelayHandler(10);

    private final ItemStack[] craftingGridItems = new ItemStack[9];
    private ItemStack recipeResult;
    private ItemStack storageType;
    private int storageAmount;

    private ConstructorState state;
    private boolean declinedState;

    public Constructor(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign) {
        super(loc, rotation, context, hasWallSign);
        loadFromStorage();
        makeNewState();
    }

    private static class ConstructorState {

        private final ItemStack[] unitStacks = new ItemStack[9];

        public ConstructorState(Constructor constructor) {
            for (int i = 0; i < 9; i++) {
                ItemStack gridItem = constructor.craftingGridItems[i];
                if (gridItem != null) {
                    ItemStack unit = gridItem.clone();
                    unit.setAmount(1);
                    unitStacks[i] = unit;
                }
            }
        }
    }

    @Override
    public void load(MechanicStorageContext context) throws Exception {
        ByteArrayInputStream str = context.getData();
        for (int i = 0; i < 9; i++) {
            this.craftingGridItems[i] = context.getSerializer().readItemStack(str);
        }

        this.recipeResult = context.getSerializer().readItemStack(str);
        this.storageType = context.getSerializer().readItemStack(str);
        this.storageAmount = context.getSerializer().readInt(str);

        if (this.storageAmount > 0 && this.storageType == null) {
            this.storageAmount = 0;
            this.recipeResult = null;
        } else if (this.storageAmount == 0 && this.storageType != null) {
            this.storageType = null;
            this.recipeResult = null;
        }
        if (this.recipeResult != null && this.recipeResult.getType() == Material.AIR) {
            this.recipeResult = null;
        }
    }

    @Override
    public void save(MechanicStorageContext context) throws SQLException, IOException {
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        for (int i = 0; i < 9; i++) {
            context.getSerializer().writeItemStack(str, this.craftingGridItems[i]);
        }

        context.getSerializer().writeItemStack(str, this.recipeResult);
        context.getSerializer().writeItemStack(str, this.storageType);
        context.getSerializer().writeInt(str, this.storageAmount);

        context.uploadData(str);
    }

    @Override
    public void onUpgrade(int newLevel) {
        this.thinkDelayHandler.setDelay(this.level.getInt(MechanicLevel.THINK_DELAY_MARK));
        super.onUpgrade(newLevel);
    }

    @Override
    public MechanicProfile<Constructor> getProfile() {
        return Profiles.CONSTRUCTOR;
    }

    @Override
    public void pipePut(ItemCollection collection, PipePutEvent event) {
        storage.ensureValidStorage();

        if (tickThrottle.isThrottled()) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack craft = craftingGridItems[i];
            // if the item is null or the stack is full except for items that can only stack to 1
            if (craft == null || (craft.getMaxStackSize() != 1 && craft.getAmount() == craft.getMaxStackSize())) {
                continue;
            }

            ItemStack unit = state.unitStacks[i];
            if (collection.has(unit)) {
                // find an item which has a lower amount than the currently checked item
                // this is done to ensure evenly distribution among the items in the crafting grid
                for (ItemStack oCraft : craftingGridItems) {
                    if (oCraft != craft
                            && oCraft != null
                            && oCraft.isSimilar(craft)
                            && oCraft.getAmount() < craft.getAmount()
                            // if the stack is not full or the stack can only stack to 1 and the amount is 1
                            && (oCraft.getAmount() < oCraft.getMaxStackSize() || (oCraft.getMaxStackSize() == 1 && oCraft.getAmount() == 1))
                    ) {
                        craft = oCraft;
                    }
                }

                // if the stack can only stack to 1,
                // increase the max stack size by 1 to allow recipes with single stack to be crafted
                int maxStackSize = craft.getMaxStackSize() == 1 ? 2 : craft.getMaxStackSize();

                List<ItemStack> stacks = collection.pipeTake(Math.min(level.getInt(UNIT_TRANSFER_AMOUNT_MARK), maxStackSize - craft.getAmount()));

                // loop through the stacks collected and try to put into the grid
                for (ItemStack stack : stacks) {
                    if (craft.isSimilar(stack)) {
                        craft.setAmount(craft.getAmount() + stack.getAmount());
                        event.setTransferred(true);
                    }
                }
            }
        }
    }

    @Override
    public int getCapacity() {
        return getCapacitySlots(level) *
                Optional.ofNullable(storageType)
                        .map(ItemStack::getMaxStackSize)
                        .orElse(64);
    }

    @Override
    public DelayHandler getThinkDelayHandler() {
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
                ConstructorGui gui = this.<ConstructorGui>getGuiInUse().get();
                if (gui != null) {
                    gui.updateDeclinedState(true);
                }
            }

            return;
        }

        // remove declined state if set and crafting is available
        if (declinedState) {
            declinedState = false;
            ConstructorGui gui = this.<ConstructorGui>getGuiInUse().get();
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
        int a = 0;
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
                a++;
                crafting.setAmount(crafting.getAmount() - 1);
            }
        }

        xp += xpDist.poll() * a;

        // update the storage type if not set
        if (storageType == null) {
            storageType = recipeResult.clone();
            storageType.setAmount(1);
        }

        storageAmount += recipeResult.getAmount();

        ConstructorGui gui = this.<ConstructorGui>getGuiInUse().get();
        if (gui != null) {
            gui.updateAddedItems(recipeResult.getAmount());
            for (HumanEntity player : gui.getInventory().getViewers()) {
                ((Player)player).playSound(getLocation(), Sound.BLOCK_WOOD_HIT, 0.5f, 1f);
            }
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
    public List<ItemStack> pipeTake(int amount) {
        storage.ensureValidStorage();

        if (tickThrottle.isThrottled() || storageType == null || storageAmount == 0) {
            return Collections.emptyList();
        }

        return this.<ConstructorGui>take((int) Math.min(getMaxTransfer(), amount), storageType, storageAmount, getGuiInUse(), ConstructorGui::updateRemovedItems, storage);
    }

    @Override
    public boolean isTransferEmpty() {
        return storageType == null;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return transferDelayHandler;
    }

    @Override
    public double getMaxTransfer() {
        return storageType.getMaxStackSize();
    }

    @Override
    public double getTransferAmount() {
        return storageAmount;
    }

    @Override
    public boolean isContainerEmpty() {
        return Arrays.stream(craftingGridItems).filter(Objects::nonNull).allMatch(i -> i.getType() == Material.AIR);
    }

    @Override
    public double getTransferEnergyCost() {
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

        if (this.storageType == null) {
            this.storageAmount = 0;
        }
    }

    public int getStorageAmount() {
        return storageAmount;
    }

    public void setStorageAmount(int amount) {
        this.storageAmount = amount;

        if (this.storageAmount == 0) {
            this.storageType = null;
        }
    }

    public void makeNewState() {
        this.state = new ConstructorState(this);
    }
}
