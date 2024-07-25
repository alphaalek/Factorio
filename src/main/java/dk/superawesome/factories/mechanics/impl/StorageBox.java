package dk.superawesome.factories.mechanics.impl;

import dk.superawesome.factories.gui.impl.StorageBoxGui;
import dk.superawesome.factories.mechanics.*;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Predicate;

public class StorageBox extends AbstractMechanic<StorageBox, StorageBoxGui> implements ItemCollection, Container {

    private ItemStack stored;
    private int amount;

    public StorageBox(Location location, BlockFace rotation, MechanicStorageContext context) {
        super(location, rotation, context);
        loadFromStorage();
    }

    @Override
    public void load(MechanicStorageContext context) throws Exception {
        ByteArrayInputStream str = context.getData();
        this.stored = context.readItemStack(str);
        this.amount = context.readInt(str);
    }

    @Override
    public void save(MechanicStorageContext context) throws Exception {
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        context.writeItemStack(str, this.stored);
        str.write(this.amount);

        context.upload(str);
    }

    @Override
    public MechanicProfile<StorageBox, StorageBoxGui> getProfile() {
        return Profiles.STORAGE_BOX;
    }

    @Override
    public void pipePut(ItemCollection collection) {
        int capacity = getCapacity();
        if (tickThrottle.isThrottled() || amount == capacity) {
            return;
        }

        if (stored == null || collection.has(stored)) {
            amount += put(collection, Math.min(64, capacity - amount), inUse, StorageBoxGui::updateAddedItems, new Updater<ItemStack>() {
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
        return level.get(ItemCollection.CAPACITY_MARK);
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
        List<ItemStack> items = take(amount, stored, amount, inUse, g -> g.updateRemovedItems(amount), new Updater<Integer>() {
            @Override
            public Integer get() {
                return StorageBox.this.amount;
            }

            @Override
            public void set(Integer val) {
                StorageBox.this.amount -= val;
            }
        });

        if (this.amount == 0) {
            this.stored = null;
        }

        return items;
    }

    @Override
    public boolean isEmpty() {
        return stored == null;
    }

    @Override
    public double getEnergyCost() {
        return 1d / 4d;
    }

    public ItemStack getStored() {
        return stored;
    }

    public void setStored(ItemStack stack) {
        this.stored = stack;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
