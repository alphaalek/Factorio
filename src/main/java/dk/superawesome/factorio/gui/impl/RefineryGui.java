package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.impl.behaviour.Refinery;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RefineryGui extends MechanicGui<RefineryGui, Refinery> {

    public static final int EMPTY_BOTTLE_CONTEXT = 0;
    public static final int FILLED_BOTTLE_CONTEXT = 1;


    public static final List<Integer> BOTTLES_SLOTS = Arrays.asList(10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39);
    public static final List<Integer> FILLED_BOTTLES_SLOTS = Arrays.asList(14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43);

    public RefineryGui(Refinery mechanic, AtomicReference<RefineryGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadItems() {
        for (int i : Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 13, 17, 18, 26, 27, 31, 35, 36, 40, 44, 45, 46, 47, 48, 49, 50)) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        getInventory().setItem(22, new ItemStack(Material.CHAIN));

        super.loadItems();
    }

    @Override
    public void loadInputOutputItems() {
    }
}
