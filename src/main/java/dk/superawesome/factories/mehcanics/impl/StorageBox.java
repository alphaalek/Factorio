package dk.superawesome.factories.mehcanics.impl;

import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.mehcanics.AbstractMechanic;
import dk.superawesome.factories.mehcanics.MechanicProfile;
import dk.superawesome.factories.mehcanics.Profiles;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class StorageBox extends AbstractMechanic<StorageBox> {

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
    public MechanicProfile<StorageBox> getProfile() {
        return Profiles.STORAGE_BOX;
    }

    @Override
    public void pipePut(ItemCollection collection) {

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
        List<ItemStack> items = new ArrayList<>();
        int taken = 0;
        while (taken < amount && taken < this.amount) {
            ItemStack item = stored.clone();
            int a = Math.min(item.getMaxStackSize(), Math.min(this.amount, amount) - taken);

            taken += a;
            item.setAmount(a);
            items.add(item);
        }
        this.amount -= taken;

        return items;
    }
}
