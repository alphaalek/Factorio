package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.gui.SingleStorageGui;
import dk.superawesome.factorio.mechanics.Storage;
import dk.superawesome.factorio.mechanics.impl.Generator;
import dk.superawesome.factorio.mechanics.Fuel;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeneratorGui extends SingleStorageGui<GeneratorGui, Generator> {

    private static final List<Integer> STORAGE_SLOTS = Arrays.asList(2, 3, 4, 5, 6, 7, 11, 12, 13, 14, 15, 16, 20, 21, 22, 23, 24, 25, 29, 30, 31, 32, 33, 34);

    public GeneratorGui(Generator mechanic, AtomicReference<GeneratorGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder(), STORAGE_SLOTS);
        initCallback.call();
    }

    @Override
    public void loadItems() {
        for (int i : Arrays.asList(1, 8, 10, 17, 19, 26, 28, 35, 37, 38, 39, 40, 41, 42, 43, 44, 46, 47, 48, 49, 50)) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }

        updateFuelState();

        super.loadItems();
    }

    @Override
    public int getContext() {
        return 0;
    }

    @Override
    protected boolean isItemAllowed(ItemStack item) {
        return Fuel.isFuel(item.getType());
    }

    public void updateFuelState() {
        updateFuelState(Stream.of(0, 9, 18, 27, 36, 45).sorted(Collections.reverseOrder()).collect(Collectors.toList()));
    }
}
