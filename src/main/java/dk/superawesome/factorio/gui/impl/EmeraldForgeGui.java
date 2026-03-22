package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.api.events.MoneyCollectEvent;
import dk.superawesome.factorio.gui.Elements;
import dk.superawesome.factorio.gui.GuiElement;
import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.impl.accessible.EmeraldForge;
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

import static dk.superawesome.factorio.util.statics.StringUtil.formatNumber;

public class EmeraldForgeGui extends MechanicGui<EmeraldForgeGui, EmeraldForge> {

    private static final List<Integer> STORAGE_SLOTS = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34);

    public EmeraldForgeGui(EmeraldForge mechanic, AtomicReference<EmeraldForgeGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadItems() {
        super.loadItems();

        for (int i : Arrays.asList(0, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 50)) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }

        registerEvent(49, this::handleTakeMoney);
    }

    private void updateMoneyAmount() {
        getInventory().setItem(49, new ItemBuilder(Material.MINECART)
                .setName("§eFå emeralder §8(§e" + formatNumber(getMechanic().getMoneyAmount()) + "/" + formatNumber(getMechanic().getCapacity()) + " i alt§8)")
                .addLore("")
                .addLore("§eKlik for at tage ud. §8(§e§oShift for alt§8)")
                .build());
    }

    @Override
    public void updateItems() {
        int amount = (int) Math.ceil(getMechanic().getMoneyAmount());

        // first load normal emeralds
        int left = loadStorageTypes(new ItemStack(Material.EMERALD), amount, STORAGE_SLOTS);
        if (left > 0) {
            // then evaluate amount of emerald blocks we can display
            int blocks = 0, slot = 0;
            while (left > 9 && slot < STORAGE_SLOTS.size()) {
                ItemStack item = getInventory().getItem(STORAGE_SLOTS.get(slot));

                blocks++;
                left -= 9;

                if (item != null && item.getAmount() == 64) {
                    left += 64;
                    item.setAmount(0);
                }

                if (blocks % 64 == 0 && left >= 9) {
                    slot++;
                } else if (left < 9) {
                    break;
                }
            }

            loadStorageTypesWithoutClear(new ItemStack(Material.EMERALD_BLOCK), blocks, STORAGE_SLOTS);
        }
        updateMoneyAmount();
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

            MoneyCollectEvent collectEvent = new MoneyCollectEvent(player, moneyAmount, getMechanic());
            Bukkit.getPluginManager().callEvent(collectEvent);
            /*if (collectEvent.isCancelled() || !collectEvent.isCollected()) {
                player.sendMessage("§cKunne ikke tage fra maskinens inventar. Kontakt en udvikler.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                return;
            } Så man kan tage ting ud af emerald forgen */

            getMechanic().setMoneyAmount(getMechanic().getMoneyAmount() - moneyAmount);
            updateItems();

            player.sendMessage("§eDu tog §f" + formatNumber(moneyAmount) + "§e emeralder fra maskinens inventar.");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1f);
        };

        if (event.getClick().isShiftClick()) {
            take.accept(getMechanic().getMoneyAmount());
        } else {
            openSignGuiAndCall(player, StringUtil.formatDecimals(getMechanic().getMoneyAmount(), 2) + "", take);
        }
    }
}
