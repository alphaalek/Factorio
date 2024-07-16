package dk.superawesome.factories.gui;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.util.Callback;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public abstract class BaseGui<G extends BaseGui<G>> implements InventoryHolder, Listener {

    static {
        Factories.get().registerEvent(InventoryCloseEvent.class, EventPriority.LOWEST, e -> {
            closeGui(e.getInventory().getHolder(), e.getPlayer());
        });

        Factories.get().registerEvent(PlayerQuitEvent.class, EventPriority.LOWEST, e -> {
            closeGui(e.getPlayer().getOpenInventory().getTopInventory().getHolder(), e.getPlayer());
        });

        Factories.get().registerEvent(InventoryDragEvent.class, EventPriority.LOWEST, e -> {
            InventoryHolder holder = e.getInventory().getHolder();
            if (holder instanceof BaseGui) {
                boolean cancelled = ((BaseGui<?>)holder).onDrag(e);
                e.setCancelled(cancelled);
            }
        });

        Factories.get().registerEvent(InventoryClickEvent.class, EventPriority.LOWEST, e -> {
            BaseGui<?> gui = null;
            Inventory inv = e.getClickedInventory();
            if (inv != null) {
                InventoryHolder holder = inv.getHolder();
                if (holder instanceof BaseGui) {
                    boolean cancelled = (gui = ((BaseGui<?>)holder)).onClickIn(e);
                    e.setCancelled(cancelled);
                }
            }

            InventoryHolder holder = e.getWhoClicked().getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof BaseGui) {
                boolean cancelled = (gui = ((BaseGui<?>)holder)).onClickOpen(e);
                if (!e.isCancelled()) {
                    e.setCancelled(cancelled);
                }
            }

            if (gui != null) {
                gui.onClickPost(e);
            }
        });
    }

    private static void closeGui(InventoryHolder holder, HumanEntity player) {
        if (holder instanceof BaseGui) {
            BaseGui<?> gui = (BaseGui<?>) holder;
            gui.onClose();

            if (holder.getInventory().getViewers().stream().noneMatch(p -> p != player)) {
                gui.clearInUse();
            }
        }
    }

    protected static int DOUBLE_CHEST = 54;
    protected boolean loaded = false;
    protected final Callback initCallback;
    protected final Inventory inventory;

    private final AtomicReference<G> inUseReference;

    public BaseGui(Supplier<Callback> initCallback, AtomicReference<G> inUseReference, int size, String title) {
        this.inventory = Bukkit.createInventory(this, size, title);
        this.initCallback = initCallback.get();
        this.initCallback.add(this::loadItems);
        this.loaded = true;
        this.inUseReference = inUseReference;
        this.inUseReference.set((G) this);
    }

    private void clearInUse() {
        inUseReference.set(null);
    }

    protected boolean movedFromOtherInventory(InventoryClickEvent event) {
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR
                || event.getAction() == InventoryAction.HOTBAR_SWAP
                || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD
                || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            Inventory topInv = event.getWhoClicked().getOpenInventory().getTopInventory();
            return topInv.getHolder() == this;
        }

        return false;
    }

    @Override
    @Nonnull
    public Inventory getInventory() {
        return inventory;
    }

    public abstract void loadItems();

    public abstract void onClose();

    public abstract boolean onDrag(InventoryDragEvent event);

    public abstract boolean onClickIn(InventoryClickEvent event);

    public abstract boolean onClickOpen(InventoryClickEvent event);

    public abstract void onClickPost(InventoryClickEvent event);
}
