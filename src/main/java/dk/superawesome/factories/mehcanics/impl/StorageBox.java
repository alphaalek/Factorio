package dk.superawesome.factories.mehcanics.impl;

import dk.superawesome.factories.gui.impl.StorageBoxGui;
import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.mehcanics.AbstractMechanic;
import dk.superawesome.factories.mehcanics.MechanicProfile;
import dk.superawesome.factories.mehcanics.Profiles;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
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
    public void openInventory(Player player) {
        StorageBoxGui gui = new StorageBoxGui(this);
        player.openInventory(gui.getInventory());
    }

    @Override
    public void pipePut(ItemCollection collection) {

    }

    @Override
    public List<ItemStack> take(ItemStack stack) {
        Bukkit.getLogger().info("Takes " + stack + ", Amount " + amount + ", Stored " + stored);

        return Collections.EMPTY_LIST;
    }
}
