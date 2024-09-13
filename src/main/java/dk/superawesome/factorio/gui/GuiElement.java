package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public interface GuiElement {

    void handle(InventoryClickEvent event, Player player, MechanicGui<?, ?> gui);

    ItemStack getItem(Mechanic<?> mechanic);
}
