package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.GuiMechanicProfile;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.MechanicLevel;
import dk.superawesome.factorio.util.helper.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
            double xpRequires = mechanic.getLevel().getDouble(MechanicLevel.XP_REQUIRES_MARK);
            double per = xpRequires / level.getMax();

            double xp = mechanic.getXP();
            for (int i = 2; i <= Math.min(5, level.getMax()); i++) {
                int slot = 1 + (i - 2) * 2;
                inventory.setItem(slot, new ItemBuilder(Material.END_CRYSTAL).setAmount(i)
                        .setName("§6Level " + i)
                        .changeLore(l -> l.addAll(mechanic.getLevel().getDescription()))
                        .build());

                for (int j = 4; j > 0; j--) {
                    int xpSlot = slot + j * 9;
                    if (xp > per * (5 - j)) {
                        inventory.setItem(xpSlot, new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
                    } else {
                        inventory.setItem(xpSlot, new ItemStack(Material.RED_STAINED_GLASS_PANE));
                    }
                }
            }
        }
    }

    @Override
    public void onClose(Player player, boolean anyViewersLeft) {
        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
            if (player.isOnline() && !player.getOpenInventory().getType().isCreatable()) {
                player.openInventory(((GuiMechanicProfile<M>)mechanic.getProfile()).getGuiFactory().create(mechanic, mechanic.getGuiInUse()).getInventory());
            }
        });
    }
}
