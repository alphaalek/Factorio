package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.gui.PaginatedGui;
import dk.superawesome.factorio.gui.SingleStorageGui;
import dk.superawesome.factorio.mechanics.impl.behaviour.Assembler;
import dk.superawesome.factorio.util.DurationFormatter;
import dk.superawesome.factorio.util.helper.ItemBuilder;
import dk.superawesome.factorio.util.statics.StringUtil;
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

public class AssemblerGui extends SingleStorageGui<AssemblerGui, Assembler> {

    public static final List<Integer> STORAGE_SLOTS = Arrays.asList(1, 2, 3, 4, 10, 11, 12, 13);
    private static final List<Integer> MONEY_SLOTS = Arrays.asList(28, 29, 30, 31, 37, 38, 39, 40, 46, 47, 48, 49);

    public AssemblerGui(Assembler mechanic, AtomicReference<AssemblerGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder(), STORAGE_SLOTS);
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
        for (int i : Arrays.asList(19, 20, 21, 22)) {
            getInventory().setItem(i, new ItemStack(Material.PURPLE_STAINED_GLASS_PANE));
        }
        getInventory().setItem(34, new ItemBuilder(Material.PAPER).setName("§eVælg sammensætning").build());
        loadAssemblerType();
        registerEvent(34, e -> openChooseAssemblerGui((Player) e.getWhoClicked()));

        super.loadItems();
    }

    public void loadAssemblerType() {
        if (getMechanic().getType() != null) {
            Assembler.Type type = getMechanic().getType();
            getInventory().setItem(16,
                    new ItemBuilder(getAssemblerTypeItem(type))
                            .makeGlowing()
                            .build());
        }
    }

    private ItemStack getAssemblerTypeItem(Assembler.Type type) {
        return new ItemBuilder(type.type().getMat())
                .addLore("").addLore("§eSammensætter §fx" + type.requires() + " §etil §f$" + StringUtil.formatDecimals(type.produces(), 2) + " §8(§f$" + (StringUtil.formatDecimals(type.produces() / type.requires(), 2)) + " §epr. item§8)")
                .addLore(format(type) + " §8Sidst opdateret " + DurationFormatter.toDuration(System.currentTimeMillis() - Assembler.Types.LAST_UPDATE) + " siden")
                .addFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    public static String format(Assembler.Type type) {
        if (type.produces() > type.type().getProduces()) {
            return "§a+" + (StringUtil.formatDecimals((type.produces() / type.type().getProduces() - 1) * 100, 2)) + "%";
        } else if (type.produces() < type.type().getProduces()) {
            return "§c-" + (StringUtil.formatDecimals((type.type().getProduces() / type.produces() - 1) * 100, 2)) + "%";
        } else {
            return "§e~0%";
        }
    }

    public void updateAddedIngredients(int amount) {
        updateAddedItems(getInventory(), amount, new ItemStack(getMechanic().getType().getMat()), STORAGE_SLOTS);
    }

    public void updateRemovedIngredients(int amount) {
        updateRemovedItems(getInventory(), amount, new ItemStack(getMechanic().getType().getMat()), reverseSlots(STORAGE_SLOTS));
    }

    public void updateAddedMoney(double amount) {
        updateAddedItems(getInventory(), (int) amount, new ItemStack(Material.EMERALD), MONEY_SLOTS);
    }

    public void updateRemovedMoney(double amount) {
        updateRemovedItems(getInventory(), (int) amount, new ItemStack(Material.EMERALD), reverseSlots(MONEY_SLOTS));
    }

    private void openChooseAssemblerGui(Player player) {
        player.openInventory(new PaginatedGui<AssemblerGui, Assembler.Type>(new BaseGui.InitCallbackHolder(), null, 36, "Vælg Sammensætning", true, 3 * 9) {

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
                for (int i : Arrays.asList(27, 28, 29, 30, 31, 32, 33, 34, 35)) {
                    getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
                }

                super.loadItems();
            }

            @Override
            public List<Assembler.Type> getValues() {
                return Assembler.Types.getTypes();
            }

            @Override
            public ItemStack getItemFrom(Assembler.Type type) {
                ItemStack item = getAssemblerTypeItem(type);
                if (getMechanic().getType() != null && getMechanic().getType().equals(type)) {
                    return new ItemBuilder(item)
                            .makeGlowing()
                            .build();
                }

                return item;
            }

            @Override
            public boolean onClickIn(InventoryClickEvent event) {
                if (event.getSlot() < 27 && event.getCurrentItem() != null) {
                    Optional<Assembler.Types> typeOptional = Assembler.Types.getTypeFromMaterial(event.getCurrentItem().getType());
                    Player player = (Player) event.getWhoClicked();
                    if (typeOptional.isPresent()) {
                        // do not allow to change the assembler type if the assembler still have items
                        if (getMechanic().getIngredientAmount() > 0) {
                            player.sendMessage("§cRyd maskinens inventar før du ændrer sammensætning.");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1);
                            return true;
                        }

                        // get the chosen assembler type and set the assembler to use it
                        Assembler.Types type = typeOptional.get();
                        if (getMechanic().getType() != null && getMechanic().getType().isTypesEqual(type)) {
                            player.sendMessage("§cMaskinen bruger allerede denne sammensætning.");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1);
                            return true;
                        }

                        player.sendMessage("§eDu har valgt sammensætningen " + type + ".");
                        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1);

                        // set the assembler type
                        getMechanic().setType(type);
                        loadView();
                    }
                }

                super.onClickIn(event);

                return true;
            }

        }.getInventory());
    }

    @Override
    public int getContext() {
        return 0;
    }

    @Override
    protected boolean isItemAllowed(ItemStack item) {
        return Assembler.Types.getTypeFromMaterial(item.getType()).isPresent();
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
