package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.api.events.MechanicUpgradeEvent;
import dk.superawesome.factorio.mechanics.AccessibleMechanic;
import dk.superawesome.factorio.mechanics.GuiMechanicProfile;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.MechanicLevel;
import dk.superawesome.factorio.util.helper.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static dk.superawesome.factorio.util.statics.StringUtil.formatNumber;

public class UpgradeMechanicGui<M extends Mechanic<M>> extends BaseGuiAdapter<UpgradeMechanicGui<M>> {

    private final M mechanic;

    public UpgradeMechanicGui(M mechanic) {
        super(new InitCallbackHolder(), null, BaseGui.DOUBLE_CHEST, "Opgradering: " + mechanic.toString(), true);
        this.mechanic = mechanic;
        initCallback.call();
    }

    @Override
    public void loadItems() {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }

        loadLevels();
    }

    private void loadLevels() {
        MechanicLevel level = mechanic.getLevel();
        if (level.getMax() > 1) {
            double xp = mechanic.getXP();
            for (int i = 2; i <= Math.min(5, level.getMax()); i++) {
                int slot = 1 + (i - 2) * 2;
                List<String> desc = new ArrayList<>();
                if (level.lvl() + 1 >= i) {
                    desc.addAll(mechanic.getLevel().getRegistry().getDescription(i));
                    desc.addAll(
                            Arrays.asList("",
                                    "§e§oKræver §b§o" + formatNumber((double)mechanic.getLevel().getRegistry().get(i - 1).getOr(MechanicLevel.XP_REQUIRES_MARK, () -> 0d)) + " XP §8§o(§b§o" + formatNumber(mechanic.getXP()) + " XP§8§o)",
                                    "§e§oKoster " + formatNumber((double)mechanic.getLevel().getRegistry().get(i - 1).get(MechanicLevel.LEVEL_COST_MARK)) + " emeralder")
                    );
                } else {
                    desc.add("§c§o???");
                }

                inventory.setItem(slot, new ItemBuilder(Material.END_CRYSTAL).setAmount(i)
                        .setName("§6Level §l" + i)
                        .changeLore(l -> l.addAll(desc))
                        .build());

                double xpRequires = (double) Optional.ofNullable(mechanic.getLevel().getRegistry().get(i - 1).get(MechanicLevel.XP_REQUIRES_MARK)).orElse(0d);
                double per = xpRequires / 4;

                int has = 0;
                for (int j = 4; j > 0; j--) {
                    int xpSlot = slot + j * 9;

                    if (level.lvl() >= i) {
                        inventory.setItem(xpSlot, new ItemStack(Material.GREEN_STAINED_GLASS_PANE));
                    } else if (per == 0 || xp > per * (5 - j)) {
                        inventory.setItem(xpSlot, new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
                        has++;
                    } else {
                        inventory.setItem(xpSlot, new ItemStack(Material.RED_STAINED_GLASS_PANE));
                    }
                }

                if (level.lvl() + 1 == i) {
                    int buySlot = slot + 5 * 9;
                    if (has == 4) {
                        inventory.setItem(buySlot, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                                .setName("§aOpgrader")
                                .addLore("§eOpgrader maskinen til §6Level §l" + i)
                                .addLore("")
                                .addLore("§e§oKoster " + formatNumber((double)mechanic.getLevel().getRegistry().get(i - 1).get(MechanicLevel.LEVEL_COST_MARK)) + " emeralder")
                                .addLore("")
                                .addLore("§c§oVær opmærksom på, at du kun")
                                .addLore("§c§ofår 75% af prisen tilbage.")
                                .build());
                    } else {
                        inventory.setItem(buySlot, new ItemBuilder(Material.BARRIER)
                                .setName("§cIkke nok §bXP")
                                .addLore("§c§oOpgradering kræver §b§o" + formatNumber((double)mechanic.getLevel().getRegistry().get(i - 1).getOr(MechanicLevel.XP_REQUIRES_MARK, () -> 0d)) + " XP §8§o(§b§o" + formatNumber(mechanic.getXP()) + " XP§8§o)")
                                .build());
                    }
                }
            }
        }
    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        if (event.getCurrentItem() != null) {
            Player player = (Player) event.getWhoClicked();
            if (event.getCurrentItem().getType() == Material.BARRIER && !player.isOp()) {
                player.sendMessage("§cDu har ikke nok §bXP §ctil at opgradere maskinen.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.5f);
                return true;
            } else if (event.getCurrentItem().getType() == Material.EXPERIENCE_BOTTLE || player.isOp()) {
                int level = -1;
                switch (event.getSlot()) {
                    case 46 -> level = 2;
                    case 48 -> level = 3;
                    case 50 -> level = 4;
                    case 52 -> level = 5;
                }
                if (level != -1) {
                    MechanicUpgradeEvent upgradeEvent = new MechanicUpgradeEvent(player, mechanic, level, (double) mechanic.getLevel().getRegistry().get(level - 1).get(MechanicLevel.LEVEL_COST_MARK));
                    Bukkit.getPluginManager().callEvent(upgradeEvent);
                    if (!upgradeEvent.isCancelled()) {
                        mechanic.setLevel(level);

                        // when closing and opening right after each other, we won't trigger the onClose logic
                        player.openInventory(new UpgradeMechanicGui<>(mechanic).getInventory());
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void onClose(Player player, boolean anyViewersLeft) {
        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
            if (player.isOnline() && !player.getOpenInventory().getType().isCreatable()) {
                ((AccessibleMechanic)mechanic).openInventory(mechanic, player);
            }
        });
    }
}
