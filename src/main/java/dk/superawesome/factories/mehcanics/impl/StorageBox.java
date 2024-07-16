package dk.superawesome.factories.mehcanics.impl;

import dk.superawesome.factories.gui.impl.StorageBoxGui;
import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.mehcanics.AbstractMechanic;
import dk.superawesome.factories.mehcanics.MechanicProfile;
import dk.superawesome.factories.mehcanics.Profiles;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class StorageBox extends AbstractMechanic<StorageBox, StorageBoxGui> {

    private ItemStack stored;
    private int amount;

    public StorageBox(Location location) {
        super(location);
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

    @Override
    public MechanicProfile<StorageBox, StorageBoxGui> getProfile() {
        return Profiles.STORAGE_BOX;
    }

    @Override
    public void pipePut(ItemCollection collection) {
        if (tickThrottle.isThrottled()) {
            return;
        }

        if (stored == null || collection.has(stored)) {
            List<ItemStack> items = collection.take(64);
            int add = 0;
            for (ItemStack item : items) {
                add += item.getAmount();

                if (this.stored == null) {
                    ItemStack type = item.clone();
                    type.setAmount(1);
                    this.stored = type;
                }
            }
            this.amount += add;

            if (add > 0) {
                StorageBoxGui gui = inUse.get();
                if (gui != null) {
                    gui.updateAddedItems(add);
                }
            }
        }
    }

    @Override
    public boolean has(ItemStack stack) {
        return stored != null
                && stored.isSimilar(stack)
                && amount >= stack.getAmount()
                && !tickThrottle.isThrottled();
    }

    @Override
    public List<ItemStack> take(int amount) {
        AtomicInteger taken = new AtomicInteger();
        List<ItemStack> items = take(amount, stored, this.amount, taken, g -> g.updateRemovedItems(amount));
        this.amount -= taken.get();
        return items;
    }
}
