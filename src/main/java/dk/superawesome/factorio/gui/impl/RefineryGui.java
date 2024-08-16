package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.gui.SingleStorageGui;
import dk.superawesome.factorio.mechanics.stackregistry.Volume;
import dk.superawesome.factorio.mechanics.impl.behaviour.Refinery;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RefineryGui extends SingleStorageGui<RefineryGui, Refinery> {

    public static final int EMPTY_BOTTLE_CONTEXT = 0;
    public static final int FILLED_BOTTLE_CONTEXT = 1;


    public static final List<Integer> BOTTLES_SLOTS = Arrays.asList(10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39);
    public static final List<Integer> FILLED_BOTTLES_SLOTS = Arrays.asList(14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43);

    public RefineryGui(Refinery mechanic, AtomicReference<RefineryGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder(), BOTTLES_SLOTS);
        initCallback.call();
    }

    @Override
    public void loadItems() {
        for (int i : Arrays.asList(0, 5, 6, 7, 8, 9, 14, 15, 16, 17, 18, 23, 24, 25, 26, 27, 32, 33, 34, 35, 36, 41, 42, 43, 44, 45, 50)) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        for (int i : Arrays.asList(19, 20, 21, 22)) {
            getInventory().setItem(i, new ItemStack(Material.BLUE_STAINED_GLASS_PANE));
        }

        super.loadItems();
    }

    @Override
    public int getContext() {
        return 0;
    }

    @Override
    protected boolean isItemAllowed(ItemStack item) {
        return Volume.getType(item.getType()).isPresent();
    }

    public void updateRemovedFilled(int amount) {

    }
}
