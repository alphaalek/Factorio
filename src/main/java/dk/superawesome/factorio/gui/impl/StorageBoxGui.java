package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.gui.SingleStorageGui;
import dk.superawesome.factorio.mechanics.impl.accessible.StorageBox;
import dk.superawesome.factorio.util.helper.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class StorageBoxGui extends SingleStorageGui<StorageBoxGui, StorageBox> {

    public static final int STORAGE_CONTEXT = 0;

    public static final int STORED_SIZE = 36;

    public StorageBoxGui(StorageBox mechanic, AtomicReference<StorageBoxGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadItems() {
        for (int i : Arrays.asList(36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 50)) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        getInventory().setItem(49, new ItemBuilder(Material.MINECART)
                .setName("§eIndsæt/Tage imellem inventar §8(§e" + getMechanic().getAmount() + "/" + getMechanic().getCapacity() + " items i alt§8)")
                .addLore("")
                .addLore("§eHøjreklik for at tage ud. §8(§e§oShift for alt§8)")
                .addLore("§eVenstreklik for at putte ind. §8(§e§oShift for alt§8)")
                .build());

        registerEvent(49, this::handlePutOrTakeAll);

        super.loadItems();
    }

    @Override
    public int getContext() {
        return 0;
    }

    @Override
    protected boolean isItemAllowed(ItemStack item) {
        return true;
    }
}
