package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.gui.Elements;
import dk.superawesome.factorio.gui.GuiElement;
import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.impl.EmeraldForge;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class EmeraldForgeGui extends MechanicGui<EmeraldForgeGui, EmeraldForge> {

    private static final List<Integer> STORAGE_SLOTS = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34);

    private int displayed;

    public EmeraldForgeGui(EmeraldForge mechanic, AtomicReference<EmeraldForgeGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadItems() {
        super.loadItems();

        for (int i : Arrays.asList(0, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 50, 53)) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }
        getInventory().setItem(49, new ItemBuilder(Material.MINECART)
                .setName("§eFå emeralder")
                .addLore("")
                .addLore("§eKlik for at tage ud. §8(§e§oShift for alt§8)")
                .build());

        registerEvent(49, this::handleTakeMoney);
    }

    @Override
    public void loadInputOutputItems() {
        int amount = (int) Math.round(getMechanic().getMoneyAmount());
        displayed += amount;
        loadStorageTypes(new ItemStack(Material.EMERALD), amount, STORAGE_SLOTS);
    }

    public void updateAddedMoney(double amount) {
        int displayAmount = (int) Math.round(amount);
        displayed += displayAmount;
        updateAddedItems(getInventory(), displayAmount, new ItemStack(Material.EMERALD), STORAGE_SLOTS);
        ensureCorrectDisplay();
    }

    private void ensureCorrectDisplay() {
        int displayAmount = (int) getMechanic().getMoneyAmount();
        if (displayed > displayAmount) {
            updateRemovedItems(getInventory(), displayed - displayAmount, new ItemStack(Material.EMERALD), STORAGE_SLOTS);
        } else if (displayed < displayAmount) {
            updateAddedItems(getInventory(), displayAmount - displayed, new ItemStack(Material.EMERALD), STORAGE_SLOTS);
        }
    }

    private void handleTakeMoney(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (getMechanic().isContainerEmpty()) {
            player.sendMessage("§cDer er intet i maskinens inventar!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
            return;
        }

        Consumer<Double> take = a -> {
            double moneyAmount = Math.min(getMechanic().getMoneyAmount(), a);

            int displayAmount = (int) Math.round(moneyAmount);
            displayed -= displayAmount;
            updateRemovedItems(getInventory(), displayAmount, new ItemStack(Material.EMERALD), STORAGE_SLOTS);
            getMechanic().setMoneyAmount(getMechanic().getMoneyAmount() - moneyAmount);
            ensureCorrectDisplay();

            player.sendMessage("§eDu tog §f$" + StringUtil.formatDecimals(moneyAmount, 2) + "§e fra maskinens inventar.");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1f);
        };

        if (event.getClick().isShiftClick()) {
            take.accept(getMechanic().getMoneyAmount());
        } else {
            openSignGuiAndCall(player, StringUtil.formatDecimals(getMechanic().getMoneyAmount(), 2) + "", take);
        }
    }

    @Override
    public List<GuiElement> getGuiElements() {
        return Arrays.asList(Elements.MEMBERS, Elements.DELETE);
    }
}
