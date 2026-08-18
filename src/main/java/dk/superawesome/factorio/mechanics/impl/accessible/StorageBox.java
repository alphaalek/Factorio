package dk.superawesome.factorio.mechanics.impl.accessible;

import dk.superawesome.factorio.gui.impl.StorageBoxGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import dk.superawesome.factorio.util.statics.StringUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static dk.superawesome.factorio.util.statics.StringUtil.formatNumber;

public class StorageBox extends AbstractMechanic<StorageBox> implements AccessibleMechanic, ItemCollection, ItemContainer, Storage {

    private final DelayHandler transferDelayHandler = new DelayHandler(10);

    private ItemStack stored;
    private int amount;

    public StorageBox(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public void loadData(ByteArrayInputStream data) throws Exception {
        this.stored = this.context.getSerializer().readItemStack(data);
        this.amount = this.context.getSerializer().readInt(data);

        ensureValidStorage();
    }

    @Override
    public Optional<ByteArrayOutputStream> saveData() throws Exception {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        this.context.getSerializer().writeItemStack(data, this.stored);
        this.context.getSerializer().writeInt(data, this.amount);

        return Optional.of(data);
    }

    @Override
    public MechanicProfile<StorageBox> getProfile() {
        return Profiles.STORAGE_BOX;
    }

    @Override
    public void pipePut(ItemCollection collection, PipePutEvent event) {
        ensureValidStorage();

        if (this.tickThrottle.isThrottled()) {
            return;
        }

        if ((this.stored == null || collection.has(this.stored)) && this.amount < getCapacity()) {
            event.setTransferred(true);
            this.amount += this.<StorageBoxGui>put(collection, getCapacity() - this.amount, getGuiInUse(), StorageBoxGui::updateAddedItems, this);
        }
    }

    public int put(int amount) {
        if (this.tickThrottle.isThrottled()) {
            return 0;
        }

        int prev = this.amount;
        this.amount = Math.min(getCapacity(), this.amount + amount);

        int added = this.amount - prev;
        if (added > 0) {
            StorageBoxGui gui = this.<StorageBoxGui>getGuiInUse().get();
            if (gui != null) {
                gui.updateAddedItems(added);
            }
        }

        return added;
    }

    @Override
    public int getCapacity() {
        return getCapacitySlots(this.level) *
                Optional.ofNullable(this.stored)
                        .map(ItemStack::getMaxStackSize)
                        .orElse(64);
    }

    @Override
    public void onUpdate() {
        Sign sign = getSign();
        sign.getSide(Side.FRONT).setLine(2, formatNumber(((double)amount) / getCapacity() * 100) + "% fyldt");
        sign.update();
    }

    @Override
    public boolean has(ItemStack stack) {
        return has(i -> i.isSimilar(stack) && amount >= stack.getAmount());
    }

    @Override
    public boolean has(Predicate<ItemStack> stack) {
        return this.stored != null && stack.test(this.stored);
    }

    @Override
    public List<ItemStack> pipeTake(int amount) {
        return take((int) Math.min(getMaxTransfer(), amount));
    }

    public List<ItemStack> take(int amount) {
        ensureValidStorage();

        if (this.tickThrottle.isThrottled() || this.stored == null || amount == 0) {
            return Collections.emptyList();
        }

        return this.<StorageBoxGui>take(amount, this.stored, this.amount, getGuiInUse(), StorageBoxGui::updateRemovedItems, this);

    }

    @Override
    public boolean isTransferEmpty() {
        return this.stored == null;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return this.transferDelayHandler;
    }

    @Override
    public double getMaxTransfer() {
        return this.stored.getMaxStackSize();
    }

    @Override
    public boolean isContainerEmpty() {
        return this.stored == null;
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d / 4d;
    }

    @Override
    public ItemStack getStored() {
        return this.stored;
    }

    @Override
    public void setStored(ItemStack stack) {
        this.stored = stack;

        if (this.stored == null) {
            this.amount = 0;
        }
    }

    @Override
    public int getAmount() {
        return this.amount;
    }

    @Override
    public double getTransferAmount() {
        return this.amount;
    }

    @Override
    public void setAmount(int amount) {
        this.amount = amount;

        if (this.amount == 0) {
            this.stored = null;
        }
    }
}
