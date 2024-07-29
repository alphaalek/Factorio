package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.Management;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.items.Container;
import dk.superawesome.factorio.mechanics.items.ItemCollection;
import dk.superawesome.factorio.util.db.Types;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.logging.Level;

public class Elements {

    public static GuiElement UPGRADE = new GuiElement() {
        @Override
        public void handle(InventoryClickEvent event, Player player, MechanicGui<?, ?> gui) {
            event.setCancelled(true);
        }

        @Override
        public ItemStack getItem() {
            return new ItemStack(Material.WRITABLE_BOOK);
        }
    };

    public static GuiElement MEMBERS = new GuiElement() {
        @Override
        public void handle(InventoryClickEvent event, Player player, MechanicGui<?, ?> gui) {
            event.setCancelled(true);
        }

        @Override
        public ItemStack getItem() {
            return new ItemStack(Material.NAME_TAG);
        }
    };

    public static GuiElement DELETE = new GuiElement() {
        @Override
        public void handle(InventoryClickEvent event, Player player, MechanicGui<?, ?> gui) {
            event.setCancelled(true);
            player.closeInventory();

            Mechanic<?, ?> mechanic = gui.getMechanic();
            // check if the player has access to remove this mechanic
            if (!mechanic.getManagement().hasAccess(player.getUniqueId(), Management.DELETE)) {
                player.sendMessage("§cDu har ikke adgang til at fjerne denne maskine!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.5f);
                return;
            }

            // check if this mechanic has any items in its inventory
            if (mechanic instanceof ItemCollection && !((ItemCollection)mechanic).isEmpty()
                    || mechanic instanceof Container && !((Container)mechanic).isContainerEmpty()) {
                player.sendMessage("§cRyd maskinens inventar før du sletter den!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.5f);
                return;
            }

            // unload and delete this mechanic
            Factorio.get().getMechanicManager(player.getWorld()).unload(mechanic);
            try {
                Factorio.get().getContextProvider().deleteAt(mechanic.getLocation());
            } catch (SQLException ex) {
                player.sendMessage("§cDer opstod en fejl! Kontakt en staff.");
                Factorio.get().getLogger().log(Level.SEVERE, "Failed to delete mechanic at location " + mechanic.getLocation(), ex);
                return;
            }
            Buildings.remove(player.getWorld(), mechanic);

            // player stuff
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.6f);
            player.sendMessage("§eDu fjernede maskinen " + mechanic.getProfile().getName() + " (Lvl " + mechanic.getLevel() + ") ved " + Types.LOCATION.convert(mechanic.getLocation()) + ".");
        }

        @Override
        public ItemStack getItem() {
            return new ItemStack(Material.RED_WOOL);
        }
    };
}
