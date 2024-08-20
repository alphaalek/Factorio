package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.impl.behaviour.LiquidTank;
import dk.superawesome.factorio.util.helper.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LiquidTankGui extends MechanicGui<LiquidTankGui, LiquidTank>{

    private static final int[][] STORAGE_SLOTS_STATES = new int[][] {
            new int[]{1, 2, 3, 4, 5, 6, 7},
            new int[]{10, 11, 12, 13, 14, 15, 16},
            new int[]{19, 20, 21, 22, 23, 24, 25},
            new int[]{28, 29, 30, 31, 32, 33, 34},
            new int[]{37, 38, 39, 40, 41, 42, 43}
    };

    private static final List<Integer> STORAGE_SLOTS;

    static {
        int[] i = {0}; // force counter to heap for lambda access
        STORAGE_SLOTS = Arrays.stream(STORAGE_SLOTS_STATES).flatMapToInt(Arrays::stream).boxed().toList();
    }

    public LiquidTankGui(LiquidTank mechanic, AtomicReference<LiquidTankGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadItems() {
        for (int i : Arrays.asList(0, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 53)) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }

        super.loadItems();
    }

    @Override
    public void loadInputOutputItems() {
        clearSlots(STORAGE_SLOTS);

        if (getMechanic().isContainerEmpty()) {
            return;
        }

        Material fluidMaterial = switch (getMechanic().getFluid()) {
            case WATER -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case LAVA -> Material.ORANGE_STAINED_GLASS_PANE;
            case SNOW -> Material.WHITE_STAINED_GLASS_PANE;
        };
        // calculate amount for each slot
        double each = ((double)getMechanic().getCapacity()) / (STORAGE_SLOTS_STATES.length * 64);
        double left = getMechanic().getFluidAmount() + .1;

        for (int i = 4; i >= 0; i--) {
            int amount = 0;
            for (int j = 0; j < 64; j++) {
                if (left > each) {
                    left -= each;
                    amount++;
                } else break;
            }

            if (amount > 0) {
                for (int j : STORAGE_SLOTS_STATES[i]) {
                    ItemStack stack = new ItemBuilder(fluidMaterial)
                            .setAmount(amount)
                            .setName("§eBeholder: " + getMechanic().getFluid())
                            .addLore("§e" + getMechanic().getFluidAmount() + "§8/§e" + getMechanic().getCapacity())
                            .build();
                    getInventory().setItem(j, stack);
                }
            }

            if (amount < 64) {
                break;
            }
        }
    }
}
