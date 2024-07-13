package dk.superawesome.factories.gui;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.util.mappings.ItemMappings;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public abstract class BaseGui implements InventoryHolder, Listener {

    protected static int DOUBLE_CHEST = 54;
    protected static ItemStack GRAY = ItemMappings.get("gray_stained_glass_pane").generateItem();
    protected static ItemStack BLACK = ItemMappings.get("black_stained_glass_pane").generateItem();

    protected boolean loaded = false;
    protected final Inventory inventory;

    public BaseGui(int size, String title) {
        this.inventory = Bukkit.createInventory(this, size, title);
        loadItems();
        loaded = true;

        Bukkit.getPluginManager().registerEvent(InventoryCloseEvent.class, this, EventPriority.LOW, (listener, event) -> {
            InventoryCloseEvent.getHandlerList().unregister(BaseGui.this);
            InventoryClickEvent.getHandlerList().unregister(BaseGui.this);
            InventoryDragEvent.getHandlerList().unregister(BaseGui.this);
        }, Factories.get());

        Bukkit.getPluginManager().registerEvent(InventoryClickEvent.class, this, EventPriority.LOW, (listener, event) -> {
            InventoryClickEvent inv = (InventoryClickEvent) event;
            if (inv.getClickedInventory() != null && inv.getClickedInventory().getHolder() == this) {
                Bukkit.broadcastMessage(inv.getSlot() + "");
                inv.setCancelled(onClick(inv));
            }
        }, Factories.get());

        Bukkit.getPluginManager().registerEvent(InventoryDragEvent.class, this, EventPriority.LOW, (listener, event) -> {
            InventoryDragEvent inv = (InventoryDragEvent) event;
            if (inv.getInventory().getHolder() == this) {
                inv.setCancelled(onDrag(inv));
            }
        }, Factories.get());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public abstract void loadItems();

    public abstract boolean onClick(InventoryClickEvent event);

    public boolean onDrag(InventoryDragEvent event) {
        return true;
    }
}
