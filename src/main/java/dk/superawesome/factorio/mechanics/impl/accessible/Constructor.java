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
import java.util.*;
import java.util.function.Predicate;

public class Constructor extends AbstractMechanic<Constructor> implements AccessibleMechanic, ThinkingMechanic, ItemCollection, ItemContainer {

    public static final int UNIT_TRANSFER_AMOUNT_MARK = 1;

    private final Storage storage = getProfile().getStorageProvider().createStorage(this, SingleStorageGui.CONTEXT);
    private final XPDist xpDist = new XPDist(100, 0.0025, 0.01);
    private final DelayHandler thinkDelayHandler = new DelayHandler(level.get(MechanicLevel.THINK_DELAY_MARK));
    private final DelayHandler transferDelayHandler = new DelayHandler(10);

    private ItemStack[] craftingGridItems; // super is not initialized yet and we trying to access it in loadData.
    private ItemStack recipeResult;
    private ItemStack storageType;
    private int storageAmount;

    private ConstructorState state;
    private boolean declinedState;

    public Constructor(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
        if (this.craftingGridItems == null) {
            this.craftingGridItems = new ItemStack[9];
        }
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
                    this.unitStacks[i] = unit;
                }
            }
        }
    }

    @Override
    public void loadData(ByteArrayInputStream data) throws Exception {
        if (this.craftingGridItems == null) {
            this.craftingGridItems = new ItemStack[9];
        }
        for (int i = 0; i < 9; i++) {
            this.craftingGridItems[i] = this.context.getSerializer().readItemStack(data);
        }

        this.recipeResult = this.context.getSerializer().readItemStack(data);
        this.storageType = this.context.getSerializer().readItemStack(data);
        this.storageAmount = this.context.getSerializer().readInt(data);

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
    public Optional<ByteArrayOutputStream> saveData() throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        for (int i = 0; i < 9; i++) {
            this.context.getSerializer().writeItemStack(data, this.craftingGridItems[i]);
        }

        this.context.getSerializer().writeItemStack(data, this.recipeResult);
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
    public MechanicProfile<Constructor> getProfile() {
        return Profiles.CONSTRUCTOR;
    }

    @Override
    public void pipePut(ItemCollection collection, PipePutEvent event) {
        this.storage.ensureValidStorage();

        if (this.tickThrottle.isThrottled()) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack craft = this.craftingGridItems[i];
            // if the item is null or the stack is full except for items that can only stack to 1
            if (craft == null || (craft.getMaxStackSize() != 1 && craft.getAmount() == craft.getMaxStackSize())) {
                continue;
            }

            ItemStack unit = this.state.unitStacks[i];
            if (collection.has(unit)) {
                // find an item which has a lower amount than the currently checked item
                // this is done to ensure evenly distribution among the items in the crafting grid
                for (ItemStack oCraft : this.craftingGridItems) {
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

                List<ItemStack> stacks = collection.pipeTake(Math.min(this.level.getInt(UNIT_TRANSFER_AMOUNT_MARK), maxStackSize - craft.getAmount()));

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
        return getCapacitySlots(this.level) *
                Optional.ofNullable(this.storageType)
                        .map(ItemStack::getMaxStackSize)
                        .orElse(64);
    }

    @Override
    public DelayHandler getThinkDelayHandler() {
        return this.thinkDelayHandler;
    }

    @Override
    public void think() {
        // check if the constructors storage has any previously crafted items which is not the that are
        // not the same as the current recipe.
        // if it has any, we can't craft the new recipe until all the previously crafted items are removed
        // from the storage.
        if (this.storageType != null && this.recipeResult != null && !this.storageType.isSimilar(this.recipeResult)) {
            // set declined state and notify the user that this crafting is not possible yet
            if (!this.declinedState) {
                this.declinedState = true;
                ConstructorGui gui = this.<ConstructorGui>getGuiInUse().get();
                if (gui != null) {
                    gui.updateDeclinedState(true);
                }
            }

            return;
        }

        // remove declined state if set and crafting is available
        if (this.declinedState) {
            this.declinedState = false;
            ConstructorGui gui = this.<ConstructorGui>getGuiInUse().get();
            if (gui != null) {
                gui.updateDeclinedState(false);
            }
        }

        // if there are not any recipe in the crafting grid, don't continue
        if (this.recipeResult == null
                // if there is no space left, don't continue
                || this.storageAmount + this.recipeResult.getAmount() > getCapacity()) {
            return;
        }

        // remove one amount from all items in the crafting grid and simulate the crafting
        int a = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack crafting = this.craftingGridItems[i];
            if (crafting != null && crafting.getAmount() == 1) {
                // re-set the amounts if the constructor did not have enough items for the recipe
                for (int j = 0; j < i; j++) {
                    ItemStack reSetCrafting = this.craftingGridItems[j];
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

        this.xp += this.xpDist.poll() * a;

        // update the storage type if not set
        if (this.storageType == null) {
            this.storageType = this.recipeResult.clone();
            this.storageType.setAmount(1);
        }

        this.storageAmount += this.recipeResult.getAmount();

        ConstructorGui gui = this.<ConstructorGui>getGuiInUse().get();
        if (gui != null) {
            gui.updateAddedItems(this.recipeResult.getAmount());
            for (HumanEntity player : gui.getInventory().getViewers()) {
                ((Player) player).playSound(getLocation(), Sound.BLOCK_WOOD_HIT, 0.5f, 1f);
            }
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
        this.storage.ensureValidStorage();

        if (this.tickThrottle.isThrottled() || this.storageType == null || this.storageAmount == 0) {
            return Collections.emptyList();
        }

        return this.<ConstructorGui>take((int) Math.min(getMaxTransfer(), amount), this.storageType, this.storageAmount, getGuiInUse(), ConstructorGui::updateRemovedItems, this.storage);
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
        return Arrays.stream(this.craftingGridItems).filter(Objects::nonNull).allMatch(i -> i.getType() == Material.AIR);
    }

    @Override
    public double getTransferEnergyCost() {
        return 2d / 3d;
    }

    public boolean isDeclined() {
        return this.declinedState;
    }

    public ItemStack[] getCraftingGridItems() {
        return this.craftingGridItems;
    }

    public ItemStack getRecipeResult() {
        return this.recipeResult;
    }

    public void setRecipeResult(ItemStack result) {
        this.recipeResult = result;
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

    public void makeNewState() {
        this.state = new ConstructorState(this);
    }
}
