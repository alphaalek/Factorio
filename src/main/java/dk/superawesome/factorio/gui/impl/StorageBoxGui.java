package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.gui.SingleStorageGui;
import dk.superawesome.factorio.mechanics.impl.behaviour.StorageBox;
import dk.superawesome.factorio.util.helper.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StorageBoxGui extends SingleStorageGui<StorageBoxGui, StorageBox> {

    private static final int STORED_SIZE = 35;

    public StorageBoxGui(StorageBox mechanic, AtomicReference<StorageBoxGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder(), IntStream.range(0, STORED_SIZE).boxed().collect(Collectors.toList()));
        initCallback.call();
    }

    @Override
    public void loadItems() {
        for (int i : Arrays.asList(36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 50)) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        getInventory().setItem(35, new ItemBuilder(Material.FEATHER).setName("§eOpdatér Inventar").build());
        getInventory().setItem(49, new ItemBuilder(Material.MINECART)
                .setName("§eIndsæt/Tage imellem inventar")
                .addLore("")
                .addLore("§eHøjreklik for at tage ud. §8(§e§oShift for alt§8)")
                .addLore("§eVenstreklik for at putte ind. §8(§e§oShift for alt§8)")
                .build());

        registerEvent(35, __ -> loadInputOutputItems());
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
