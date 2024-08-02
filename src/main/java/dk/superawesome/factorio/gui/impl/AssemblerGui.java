package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.gui.BaseGuiAdapter;
import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.impl.Assembler;
import dk.superawesome.factorio.util.helper.ItemBuilder;
import dk.superawesome.factorio.util.statics.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class AssemblerGui extends MechanicGui<AssemblerGui, Assembler> {

    private static final List<Integer> STORAGE_SLOTS = Arrays.asList(1, 2, 3, 4, 10, 11, 12, 13);
    private static final List<Integer> MONEY_SLOTS = Arrays.asList(28, 29, 30, 31, 37, 38, 39, 40);

    public AssemblerGui(Assembler mechanic, AtomicReference<AssemblerGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadItems() {
        for (int i : Arrays.asList(6, 8, 24, 26, 33, 35)) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        for (int i : Arrays.asList(0, 5, 9, 14, 18, 23, 27, 32, 36, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50)) {
            getInventory().setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        }
        for (int i : Arrays.asList(7, 15, 17, 25)) {
            getInventory().setItem(i, new ItemStack(Material.RED_STAINED_GLASS_PANE));
        }
        for (int i : Arrays.asList(19, 20, 21, 22)) {
            getInventory().setItem(i, new ItemStack(Material.PURPLE_STAINED_GLASS_PANE));
        }
        getInventory().setItem(34, new ItemBuilder(Material.PAPER).setName("§eVælg sammensætning").build());
        if (getMechanic().getType() != null) {
            getInventory().setItem(16, new ItemStack(getMechanic().getType().getMat()));
        }
        registerEvent(34, e -> openChooseAssemblerGui((Player) e.getWhoClicked()));

        super.loadItems();
    }

    public void updateAddedIngredients(int amount) {
        updateAddedItems(getInventory(), amount, new ItemStack(getMechanic().getType().getMat()), STORAGE_SLOTS);
    }

    public void updateRemovedIngredients(int amount) {
        updateRemovedItems(getInventory(), amount, new ItemStack(getMechanic().getType().getMat()), STORAGE_SLOTS);
    }

    public void updateAddedMoney(double amount) {
        updateAddedItems(getInventory(), (int) amount, new ItemStack(Material.EMERALD), MONEY_SLOTS);
    }

    public void updateRemovedMoney(double amount) {
        updateRemovedItems(getInventory(), (int) amount, new ItemStack(Material.EMERALD), MONEY_SLOTS);
    }

    private void openChooseAssemblerGui(Player player) {
        player.openInventory(new BaseGuiAdapter<AssemblerGui>(new BaseGui.InitCallbackHolder(), null, 36, "Vælg Sammensætning", true) {

            {
                // call init callback when loaded
                initCallback.call();
            }

            @Override
            public void onClose(Player player) {
                Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                    if (player.isOnline()) {
                        getMechanic().openInventory(player);
                    }
                });
            }

            @Override
            public void loadItems() {
                for (int i : Arrays.asList(27, 28, 29, 30, 31, 32, 33, 34, 35)) {
                    getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
                }

                loadAssemblerTypes();

                super.loadItems();
            }

            private void loadAssemblerTypes() {
                int i = 0;
                for (Assembler.Types type : Assembler.Types.values()) {
                    ItemStack item = new ItemStack(type.getMat());
                    if (getMechanic().getType() != null && getMechanic().getType().equals(type)) {
                        item = new ItemBuilder(item)
                                .makeGlowing()
                                .build();
                    }
                    item = new ItemBuilder(item)
                            .addLore("").addLore("§eSammensætter §fx" + type.getRequires() + " §etil §f$" + type.getProduces() + " §8(§f$" + (StringUtil.formatDecimals(type.getProduces() / type.getRequires(), 2)) + " §epr. item§8)")
                            .build();

                    getInventory().setItem(i++, item);
                }
            }

            @Override
            public boolean onClickIn(InventoryClickEvent event) {
                if (event.getSlot() < 27 && event.getCurrentItem() != null) {
                    Optional<Assembler.Types> typeOptional = Assembler.Types.getType(event.getCurrentItem().getType());
                    Player player = (Player) event.getWhoClicked();
                    if (typeOptional.isPresent()) {
                        // do not allow to change the assembler type if the assembler still have items
                        if (getMechanic().getIngredientAmount() > 0) {
                            player.sendMessage("§Ryd maskinens inventar før du ændrer sammensætning.");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1);
                            return true;
                        }

                        // get the chosen assembler type and set the assembler to use it
                        Assembler.Types type = typeOptional.get();
                        if (getMechanic().getType() != null && getMechanic().getType().equals(type)) {
                            player.sendMessage("§cMaskinen bruger allerede denne sammensætning.");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1);
                            return true;
                        }

                        player.sendMessage("§eDu har valgt sammensætningen " + type + ".");
                        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1);

                        // set the assembler type
                        getMechanic().setType(type);
                        loadAssemblerTypes();
                    }
                }

                return true;
            }

        }.getInventory());
    }

    @Override
    public void loadInputOutputItems() {
        if (getMechanic().getType() != null) {
            loadStorageTypes(new ItemStack(getMechanic().getType().getMat()), getMechanic().getIngredientAmount(), STORAGE_SLOTS);
        }
        int moneyAmount = (int) Math.round(getMechanic().getMoneyAmount());
        if (moneyAmount > 0) {
            loadStorageTypes(new ItemStack(Material.EMERALD), moneyAmount, MONEY_SLOTS);
        }
    }
}
