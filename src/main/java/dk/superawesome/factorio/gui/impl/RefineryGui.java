package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.gui.SingleStorageGui;
import dk.superawesome.factorio.mechanics.impl.accessible.Refinery;
import dk.superawesome.factorio.mechanics.stackregistry.Fluid;
import dk.superawesome.factorio.mechanics.stackregistry.Volume;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RefineryGui extends SingleStorageGui<RefineryGui, Refinery> {

    public static final int VOLUME_CONTEXT = 0;
    public static final int FILLED_CONTEXT = 1;

    public static final List<Integer> VOLUME_SLOTS = Arrays.asList(1, 2, 3, 4, 10, 11, 12, 13);
    public static final List<Integer> FILLED_SLOTS = Arrays.asList(28, 29, 30, 31, 37, 38, 39, 40, 46, 47, 48, 49);

    public RefineryGui(Refinery mechanic, AtomicReference<RefineryGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadItems() {
        for (int i : Arrays.asList(0, 5, 6, 7, 8, 9, 14, 15, 16, 17, 18, 23, 24, 25, 26, 27, 32, 33, 34, 35, 36, 41, 42, 43, 44, 45, 50)) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        loadFluidGlass();

        super.loadItems();
    }

    private void loadFluidGlass() {
        if (getMechanic().getFilled() == null)
            for (int i : Arrays.asList(19, 20, 21, 22)) {
                getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            }
        else {
            Fluid fluid = getMechanic().getFilled().getFluid();
            ItemStack fluidGlass = switch (fluid) {
                case WATER -> new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
                case LAVA -> new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
                case SNOW -> new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
            };
            for (int i : Arrays.asList(19, 20, 21, 22)) {
                getInventory().setItem(i, fluidGlass);
            }
        }
    }

    @Override
    public void updateItems() {
        super.updateItems();

        if (getMechanic().getFilled() != null) {
            loadStorageTypes(getMechanic().getFilled().getOutputItemStack(), getMechanic().getFilledAmount(), FILLED_SLOTS);
        }
    }

    @Override
    public int getContext() {
        return VOLUME_CONTEXT;
    }

    @Override
    protected boolean isItemAllowed(ItemStack item) {
        return Volume.getTypeFromMaterial(item.getType()).isPresent();
    }

    public void updateAddedVolume(int amount) {
        updateAddedItems(getInventory(), amount, new ItemStack(getMechanic().getVolume().getMat()), VOLUME_SLOTS);
    }

    public void updateRemovedVolume(int amount) {
        updateRemovedItems(getInventory(), amount, new ItemStack(getMechanic().getVolume().getMat()), reverseSlots(VOLUME_SLOTS));
    }

    public void updateAddedFilled(int amount) {
        updateAddedItems(getInventory(), amount, getMechanic().getFilled().getOutputItemStack(), FILLED_SLOTS);
    }

    public void updateRemovedFilled(int amount) {
        updateRemovedItems(getInventory(), amount, getMechanic().getFilled().getOutputItemStack(), reverseSlots(FILLED_SLOTS));
    }
}
