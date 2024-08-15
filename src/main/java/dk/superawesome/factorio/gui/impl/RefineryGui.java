package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.gui.PaginatedGui;
import dk.superawesome.factorio.gui.SingleStorageGui;
import dk.superawesome.factorio.mechanics.impl.behaviour.Refinery;
import dk.superawesome.factorio.util.helper.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class RefineryGui extends SingleStorageGui<RefineryGui, Refinery> {

    public static final int EMPTY_BOTTLE_CONTEXT = 0;
    public static final int FILLED_BOTTLE_CONTEXT = 1;


    public static final List<Integer> BOTTLES_SLOTS = Arrays.asList(1, 2, 3, 4, 10, 11, 12, 13);
    public static final List<Integer> FILLED_BOTTLES_SLOTS = Arrays.asList(28, 29, 30, 31, 37, 38, 39, 40, 46, 47, 48, 49);

    public RefineryGui(Refinery mechanic, AtomicReference<RefineryGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder(), BOTTLES_SLOTS);
        initCallback.call();
    }

    @Override
    public void loadItems() {
        for (int i : Arrays.asList(0, 5, 6, 8, 9, 14, 18, 23, 24, 26, 27, 32, 33, 35, 36, 41, 42, 43, 44, 45, 50)) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        for (int i : Arrays.asList(7, 15, 17, 25)) {
            getInventory().setItem(i, new ItemStack(Material.RED_STAINED_GLASS_PANE));
        }
        loadLiquidSlots();
        getInventory().setItem(34, new ItemBuilder(Material.PAPER).setName("§eVælg flaske").build());
        loadBottles();
        registerEvent(34, e -> openChooseBottleGui((Player) e.getWhoClicked()));

        super.loadItems();
    }

    public void loadLiquidSlots() {
        ItemStack liquidGlass;
        if (getMechanic().getBottleResult() != null)
            liquidGlass = getMechanic().getBottleResult().getLiquidStack().getType().equals(Material.LAVA) ? new ItemStack(Material.ORANGE_STAINED_GLASS_PANE) : new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        else
            liquidGlass = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);

        for (int i : Arrays.asList(19, 20, 21, 22)) {
            getInventory().setItem(i, liquidGlass);
        }
    }

    private void openChooseBottleGui(Player player) {
        player.openInventory(new PaginatedGui<RefineryGui, Refinery.Bottle>(new BaseGui.InitCallbackHolder(), null, 2 * 9, "Vælg flaske", true, 1 * 9) {

            {
                // call init callback when loaded
                initCallback.call();
            }

            @Override
            public void onClose(Player player, boolean anyViewersLeft) {
                Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                    if (player.isOnline()) {
                        getMechanic().openInventory(getMechanic(), player);
                    }
                });
            }

            @Override
            public void loadItems() {
                //9-17
                for (int i = 9; i < 18; i++) {
                    getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
                }
                super.loadItems();
            }

            @Override
            public List<Refinery.Bottle> getValues() {
                return Refinery.Bottle.getBottles();
            }

            @Override
            public ItemStack getItemFrom(Refinery.Bottle bottle) {
                ItemStack item = getBottleItem(bottle);
                if (getMechanic().getBottleResult() != null && getMechanic().getBottleResult().equals(bottle)) {
                    return new ItemBuilder(item)
                        .makeGlowing()
                        .build();
                }

                return item;
            }

            @Override
            public boolean onClickIn(InventoryClickEvent event) {
                if (event.getSlot() < 27 && event.getCurrentItem() != null) {
                    Optional<Refinery.Bottle> bottleOptional = Refinery.Bottle.getBottleFromMaterial(event.getCurrentItem().getType());
                    Player player = (Player) event.getWhoClicked();
                    if (bottleOptional.isPresent()) {
                        // do not allow to change the bottle type if the refinery still have items
                        if (getMechanic().getBottleAmount() > 0 || getMechanic().getStorageAmount() > 0) {
                            player.sendMessage("§cRyd maskinens inventar før du ændrer flaske sammensætning.");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1);
                            return true;
                        }

                        // get the chosen bottle type and set the refinery to use it
                        Refinery.Bottle bottle = bottleOptional.get();
                        if (getMechanic().getBottleResult() != null && getMechanic().getBottleResult().equals(bottle)) {
                            player.sendMessage("§cMaskinen bruger allerede denne flaske sammensætning.");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1);
                            return true;
                        }

                        player.sendMessage("§eDu har valgt sammensætningen " + bottle + ".");
                        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1);

                        // set the bottle result
                        getMechanic().setBottleResult(bottle);
                        loadView();
                    }
                }

                super.onClickIn(event);

                return true;
            }

        }.getInventory());
    }

    public void loadBottles() {
        if (getMechanic().getBottleResult() != null) {
            Refinery.Bottle bottle = getMechanic().getBottleResult();
            getInventory().setItem(16,
                new ItemBuilder(getBottleItem(bottle))
                    .makeGlowing()
                    .build());
        }
    }

    private ItemStack getBottleItem(Refinery.Bottle bottle) {
        return new ItemBuilder(bottle.getOutputStack())
            .addFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    @Override
    public int getContext() {
        return 0;
    }

    @Override
    protected boolean isItemAllowed(ItemStack item) {
        return false;
    }

    @Override
    public void loadInputOutputItems() {

    }

    public void updateDeclinedState(boolean declined) {
        if (declined) {
            for (int i : Arrays.asList(14, 23, 32)) {
                getInventory().setItem(i, new ItemStack(Material.BARRIER));
            }
        } else {
            for (int i : Arrays.asList(14, 23, 32)) {
                getInventory().setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
            }
        }
    }
}
