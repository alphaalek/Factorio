package dk.superawesome.factories.gui;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.util.Callback;
import dk.superawesome.factories.util.mappings.ItemMappings;
import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BaseGui implements InventoryHolder, Listener {

    protected static int DOUBLE_CHEST = 54;
    protected static ItemStack GRAY = ItemMappings.get("gray_stained_glass_pane").generateItem();
    protected static ItemStack BLACK = ItemMappings.get("black_stained_glass_pane").generateItem();

    protected boolean loaded = false;
    protected final Callback initCallback;
    protected final Inventory inventory;
    protected final List<HandlerList> listeners = new ArrayList<>();

    public BaseGui(Supplier<Callback> initCallback, int size, String title) {
        this.inventory = Bukkit.createInventory(this, size, title);
        this.initCallback = initCallback.get();
        this.initCallback.add(this::loadItems);
        loaded = true;

        registerListener(InventoryCloseEvent.class, InventoryCloseEvent.getHandlerList(), e -> {
            for (HandlerList registered : listeners) {
                registered.unregister(BaseGui.this);
            }

            onClose();
        });

        registerListener(InventoryClickEvent.class, InventoryClickEvent.getHandlerList(), e -> {
            if (e.getClickedInventory() != null && e.getClickedInventory().getHolder() == this) {
                Bukkit.broadcastMessage(e.getSlot() + "");
                e.setCancelled(onClick(e));
            }
        });
    }

    protected <E extends Event> void registerListener(Class<E> clazz, HandlerList handlerList, Consumer<E> listener) {
        Bukkit.getPluginManager().registerEvent(clazz, this, EventPriority.LOW, new EventExecutor() {
            @Override
            public void execute(@Nonnull Listener l, @Nonnull Event event) throws EventException {
                listener.accept((E) event);
            }
        }, Factories.get());

        listeners.add(handlerList);
    }

    @Override
    @Nonnull
    public Inventory getInventory() {
        return inventory;
    }

    public abstract void loadItems();

    public abstract void onClose();

    public abstract boolean onClick(InventoryClickEvent event);
}
