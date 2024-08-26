package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.gui.BaseGuiAdapter;
import dk.superawesome.factorio.mechanics.GuiMechanicProfile;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.util.helper.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class UpgradeMechanicGui<M extends Mechanic<M>> extends BaseGuiAdapter<UpgradeMechanicGui<M>> {

    private final M mechanic;

    public UpgradeMechanicGui(M mechanic) {
        super(new InitCallbackHolder(), null, BaseGui.DOUBLE_CHEST, "Opgradering: " + mechanic.toString(), true);
        this.mechanic = mechanic;
        initCallback.call();
    }

    @Override
    public void loadItems() {
        for (int i : Arrays.asList(0, 2, 4, 6, 8, 9, 11, 13, 15, 17, 18, 20, 22, 24, 26, 27, 29, 31, 33, 35, 36, 38, 40, 42, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53)) {
            getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }

        for (int i = 2; i <= 5; i++) {
            getInventory().setItem(1 + (i - 2) * 2, new ItemBuilder(Material.END_CRYSTAL).setAmount(i).build());
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
