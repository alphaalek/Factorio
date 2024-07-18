package dk.superawesome.factories.mechanics.impl;

import dk.superawesome.factories.gui.impl.StorageBoxGui;
import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.mechanics.AbstractMechanic;
import dk.superawesome.factories.mechanics.MechanicProfile;
import dk.superawesome.factories.mechanics.Profiles;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Predicate;

public class StorageBox extends AbstractMechanic<StorageBox, StorageBoxGui> {

    private ItemStack stored;
    private int amount;

    public StorageBox(Location location, BlockFace rotation) {
        super(location, rotation);
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
            amount += takeItemsFrom(collection, StorageBoxGui::updateAddedItems, new Updater<ItemStack>() {
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
    public boolean has(ItemStack stack) {
        return has(i -> i.isSimilar(stack) && amount >= stack.getAmount());
    }

    @Override
    public boolean has(Predicate<ItemStack> stack) {
        return stored != null && stack.test(stored);
    }

    @Override
    public List<ItemStack> take(int amount) {
        List<ItemStack> items = take(amount, stored, amount, g -> g.updateRemovedItems(amount), new Updater<Integer>() {
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
