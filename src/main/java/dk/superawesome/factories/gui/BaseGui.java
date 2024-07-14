package dk.superawesome.factories.gui;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.util.Callback;
import dk.superawesome.factories.util.mappings.ItemMappings;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public abstract class BaseGui implements InventoryHolder, Listener {

    static {
        Factories.get().registerEvent(InventoryCloseEvent.class, EventPriority.LOWEST, e -> {
            InventoryHolder holder = e.getInventory().getHolder();
            if (holder instanceof BaseGui) {
                ((BaseGui)holder).onClose();
            }
        });

        Factories.get().registerEvent(PlayerQuitEvent.class, EventPriority.LOWEST, e -> {
            InventoryHolder holder = e.getPlayer().getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof BaseGui) {
                ((BaseGui)holder).onClose();
            }
        });

        Factories.get().registerEvent(InventoryClickEvent.class, EventPriority.LOWEST, e -> {
            boolean cancelled = false;

            Inventory inv = e.getClickedInventory();
            if (inv != null) {
                InventoryHolder holder = inv.getHolder();
                if (holder instanceof BaseGui) {
                    cancelled = ((BaseGui)holder).onClickIn(e);
                }
            }

            InventoryHolder holder = e.getWhoClicked().getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof BaseGui) {
                cancelled = ((BaseGui)holder).onClickOpen(e);
            }

            e.setCancelled(cancelled);
        });
    }

    protected static int DOUBLE_CHEST = 54;
    protected static ItemStack GRAY = ItemMappings.get("gray_stained_glass_pane").generateItem();
    protected static ItemStack BLACK = ItemMappings.get("black_stained_glass_pane").generateItem();

    protected boolean loaded = false;
    protected final Callback initCallback;
    protected final Inventory inventory;

    public BaseGui(Supplier<Callback> initCallback, int size, String title) {
        this.inventory = Bukkit.createInventory(this, size, title);
        this.initCallback = initCallback.get();
        this.initCallback.add(this::loadItems);
        loaded = true;
    }

    @Override
    @Nonnull
    public Inventory getInventory() {
        return inventory;
    }

    public abstract void loadItems();

    public abstract void onClose();

    public abstract boolean onClickIn(InventoryClickEvent event);

    public abstract boolean onClickOpen(InventoryClickEvent event);
}
