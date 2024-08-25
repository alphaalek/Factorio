package dk.superawesome.factorio.mechanics.impl.accessible;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.impl.StorageBoxGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class StorageBox extends AbstractMechanic<StorageBox> implements AccessibleMechanic, ItemCollection, ItemContainer, Storage {

    private final DelayHandler transferDelayHandler = new DelayHandler(10);

    private ItemStack stored;
    private int amount;

    public StorageBox(Location location, BlockFace rotation, MechanicStorageContext context) {
        super(location, rotation, context);
        loadFromStorage();
    }

    @Override
    public void load(MechanicStorageContext context) throws Exception {
        ByteArrayInputStream str = context.getData();
        this.stored = context.getSerializer().readItemStack(str);
        this.amount = context.getSerializer().readInt(str);

        if (this.amount < 0) {
            Factorio.get().getLogger().warning("Storage amount for "+getProfile().getName()+" at " + getLocation() + " was negative, setting to 0");
            this.amount = 0;
        }
        if (this.stored == null && this.amount > 0) {
            this.amount = 0;
        } else if (this.stored != null && this.amount == 0) {
            this.stored = null;
        }
    }

    @Override
    public void save(MechanicStorageContext context) throws Exception {
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        context.getSerializer().writeItemStack(str, this.stored);
        context.getSerializer().writeInt(str, this.amount);

        context.uploadData(str);
    }

    @Override
    public MechanicProfile<StorageBox> getProfile() {
        return Profiles.STORAGE_BOX;
    }

    @Override
    public void pipePut(ItemCollection collection, PipePutEvent event) {
        if (tickThrottle.isThrottled()) {
            return;
        }

        if ((stored == null || collection.has(stored)) && amount < getCapacity()) {
            event.setTransferred(true);
            amount += this.<StorageBoxGui>put(collection, getCapacity() - amount, getGuiInUse(), StorageBoxGui::updateAddedItems, new HeapToStackAccess<ItemStack>() {
                @Override
                public ItemStack get() {
                    return stored;
                }

                @Override
                public void set(ItemStack stack) {
                    stored = stack;
                }
            });
        }
    }

    @Override
    public int getCapacity() {
        return level.getInt(ItemCollection.CAPACITY_MARK) *
                Optional.ofNullable(stored)
                        .map(ItemStack::getMaxStackSize)
                        .orElse(64);
    }

    @Override
    public boolean has(ItemStack stack) {
        return has(i -> i.isSimilar(stack) && amount >= stack.getAmount());
    }

    @Override
    public boolean has(Predicate<ItemStack> stack) {
        return stored != null && stack.test(stored);
    }

    @Override
    public List<ItemStack> take(int amount) {
        return this.<StorageBoxGui>take(Math.min(getMaxTransfer(), amount), stored, this.amount, getGuiInUse(), StorageBoxGui::updateRemovedItems, new HeapToStackAccess<>() {
            @Override
            public Integer get() {
                return StorageBox.this.amount;
            }

            @Override
            public void set(Integer val) {
                setAmount(StorageBox.this.amount - val);
            }
        });
    }

    @Override
    public boolean isTransferEmpty() {
        return stored == null;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return transferDelayHandler;
    }

    @Override
    public int getMaxTransfer() {
        return stored.getMaxStackSize();
    }

    @Override
    public boolean isContainerEmpty() {
        return stored == null;
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d / 4d;
    }

    public ItemStack getStored() {
        return stored;
    }

    public void setStored(ItemStack stack) {
        this.stored = stack;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    public int getTransferAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;

        if (this.amount == 0) {
            stored = null;
        }
    }
}
