package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.gui.PaginatedGui;
import dk.superawesome.factorio.gui.RecipeGui;
import dk.superawesome.factorio.gui.SingleStorageGui;
import dk.superawesome.factorio.mechanics.impl.accessible.Assembler;
import dk.superawesome.factorio.util.DurationFormatter;
import dk.superawesome.factorio.util.helper.ItemBuilder;
import dk.superawesome.factorio.util.statics.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static dk.superawesome.factorio.util.statics.StringUtil.formatNumber;

public class AssemblerGui extends SingleStorageGui<AssemblerGui, Assembler> {

    public static final List<Integer> STORAGE_SLOTS = Arrays.asList(1, 2, 3, 4, 10, 11, 12);//, 13);
    private static final List<Integer> MONEY_SLOTS = Arrays.asList(28, 29, 30, 31, 37, 38, 39, 40, 46, 47, 48, 49);

    public AssemblerGui(Assembler mechanic, AtomicReference<AssemblerGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
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

        setupHandlePutOrTakeStorageStack(13, getStorage(getContext()), STORAGE_SLOTS, true, true);

        super.loadItems();
    }

    @Override
    public int getContext() {
        return CONTEXT;
    }

    @Override
    protected boolean isItemAllowed(ItemStack item) {
        return Assembler.Types.getTypeFromMaterial(item.getType()).isPresent();
    }

    @Override
    public void updateItems() {
        int moneyAmount = (int) Math.round(getMechanic().getMoneyAmount());
        if (moneyAmount > 0) {
            loadStorageTypes(new ItemStack(Material.EMERALD), moneyAmount, MONEY_SLOTS);
        }

        super.updateItems();
    }

    public void loadAssemblerType() {
        if (getMechanic().getType() != null) {
            Assembler.Type type = getMechanic().getType();
            getInventory().setItem(16,
                    new ItemBuilder(getAssemblerTypeItem(type, false))
                            .makeGlowing()
                            .build());
        }
    }

    private ItemStack getAssemblerTypeItem(Assembler.Type type, boolean isChooseAssembler) {
        ItemBuilder itemBuilder = new ItemBuilder(type.getType().getMat())
            .addLore("").addLore("§eSammensætter §fx" + type.getRequires() + " §etil §f$" + formatNumber(type.getProduces()) + " §8(§f$" + (formatNumber(type.getPricePerItem())) + " §epr. item§8)")
            .addLore(format(type) + " §8Sidst opdateret " + DurationFormatter.toDuration(System.currentTimeMillis() - Assembler.Types.LAST_UPDATE) + " siden")
            .addFlags(ItemFlag.HIDE_ATTRIBUTES);

        if (isChooseAssembler) {
            boolean hasRecipe = Bukkit.getRecipesFor(new ItemStack(type.getType().getMat())).stream().findFirst().isPresent();
            if (hasRecipe) {
                itemBuilder.addLore("").addLore("").addLore("§7§oHøjreklik for at se opskrift");
            }
        }

        return itemBuilder.build();
    }

    public static String format(Assembler.Type type) {
        if (type.getProduces() > type.getType().getProduces()) {
            return "§a+" + (formatNumber((type.getProduces() / type.getType().getProduces() - 1) * 100)) + "%";
        } else if (type.getProduces() < type.getType().getProduces()) {
            return "§c-" + (formatNumber((type.getType().getProduces() / type.getProduces() - 1) * 100)) + "%";
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

    public void setDisplayedMoney(double amount) {
        clearSlots(MONEY_SLOTS);
        updateAddedItems(getInventory(), (int) amount, new ItemStack(Material.EMERALD), MONEY_SLOTS);
    }

    private void openChooseAssemblerGui(Player player) {
        player.openInventory(new PaginatedGui<AssemblerGui, Assembler.Type>(new BaseGui.InitCallbackHolder(), null, 36, "Vælg Sammensætning", true, 3 * 9) {

            {
                // call init callback when loaded
                this.initCallback.call();
            }

            private boolean isRecipeView = false;

            @Override
            public void onClose(Player player, boolean anyViewersLeft) {
                if (this.isRecipeView) {
                    super.onClose(player, anyViewersLeft);
                    return;
                }
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
                ItemStack item = getAssemblerTypeItem(type, true);
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
                        // check if the player right-clicked to view the recipe
                        if (event.isRightClick()) {
                            Recipe recipe = Bukkit.getRecipesFor(event.getCurrentItem()).stream()
                                    .filter(r -> r instanceof CraftingRecipe)
                                    .findFirst()
                                    .orElse(null);
                            if (recipe != null) {
                                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1);
                                this.isRecipeView = true;
                                RecipeGui<Recipe> recipeRecipeGui = new RecipeGui<>(recipe);
                                player.openInventory(recipeRecipeGui.getInventory());
                                return true;
                            }
                        }

                        // do not allow to change the assembler type if the assembler still have items
                        if (getMechanic().getIngredientAmount() > 0) {
                            player.sendMessage("§cRyd maskinens inventar før du ændrer sammensætning.");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1);
                            return true;
                        }

                        // get the chosen assembler type and set the assembler to use it
                        Assembler.Types type = typeOptional.get();
                        if (getMechanic().getType() != null && getMechanic().getType().isTypesEquals(type)) {
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
}
