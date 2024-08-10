package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.util.Callback;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BaseGui<G extends BaseGui<G>> implements InventoryHolder, Listener {

    public static class InitCallbackHolder implements Supplier<Callback> {

        private final Callback initCallback = new Callback();

        @Override
        public Callback get() {
            return initCallback;
        }
    }

    static {
        Factorio.get().registerEvent(InventoryCloseEvent.class, EventPriority.LOWEST, e -> {
            closeGui(e.getInventory().getHolder(), (Player) e.getPlayer());
        });

        Factorio.get().registerEvent(PlayerQuitEvent.class, EventPriority.LOWEST, e -> {
            closeGui(e.getPlayer().getOpenInventory().getTopInventory().getHolder(), e.getPlayer());
        });

        Factorio.get().registerEvent(InventoryDragEvent.class, EventPriority.LOWEST, e -> {
            InventoryHolder holder = e.getInventory().getHolder();
            if (holder instanceof BaseGui) {
                boolean cancelled = ((BaseGui<?>)holder).onDrag(e);
                e.setCancelled(cancelled);
            }
        });

        Factorio.get().registerEvent(InventoryClickEvent.class, EventPriority.LOWEST, e -> {
            BaseGui<?> gui = null;
            Inventory inv = e.getClickedInventory();
            if (inv != null) {
                InventoryHolder holder = inv.getHolder();
                if (holder instanceof BaseGui) {
                    boolean cancelled = (gui = ((BaseGui<?>)holder)).onClickIn(e);
                    e.setCancelled(cancelled);

                    ((BaseGui<?>)holder).callEvents(e);
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

    private static void closeGui(InventoryHolder holder, Player player) {
        if (holder instanceof BaseGui<?> gui) {
            gui.onClose(player, gui.handleClose(player));

            // play close sound if the player didn't instantly open a new inventory
            Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                if (player.getOpenInventory().getTopInventory().getType() == InventoryType.PLAYER) {
                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 0.5f);
                }
            });
        }
    }

    public boolean handleClose(Player player) {
        boolean anyViewersLeft = inventory.getViewers().stream().anyMatch(p -> p != player);

        // clear usage if no other player has this gui open
        if (!anyViewersLeft) {
            clearInUse();
        }

        return anyViewersLeft;
    }

    protected static int DOUBLE_CHEST = 54;
    protected boolean loaded = false;
    protected final Callback initCallback;
    protected final Inventory inventory;
    private final Map<Integer, List<Consumer<InventoryClickEvent>>> eventHandlers = new HashMap<>();

    private final AtomicReference<G> inUseReference;

    public BaseGui(Supplier<Callback> initCallback, AtomicReference<G> inUseReference, int size, String title) {
        this.inventory = Bukkit.createInventory(this, size, title);
        this.initCallback = initCallback.get();
        this.initCallback.add(this::loadItems);
        this.loaded = true;

        this.inUseReference = inUseReference;
        if (inUseReference != null) {
            this.inUseReference.set((G) this);
        }
    }

    protected void registerEvent(int slot, Consumer<InventoryClickEvent> handler) {
        eventHandlers.computeIfAbsent(slot, d -> new ArrayList<>()).add(handler);
    }

    protected void callEvents(InventoryClickEvent event) {
        if (eventHandlers.containsKey(event.getRawSlot())) {
            for (Consumer<InventoryClickEvent> handler : eventHandlers.get(event.getRawSlot())) {
                handler.accept(event);
            }

            ((Player)event.getWhoClicked()).playSound(event.getWhoClicked().getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 0.5f);
        }
    }

    private void clearInUse() {
        if (inUseReference != null) {
            inUseReference.set(null);
        }
    }

    protected boolean movedFromOtherInventory(InventoryClickEvent event) {
        if (event.getClickedInventory() != null &&
                event.getAction() == InventoryAction.COLLECT_TO_CURSOR
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

    public abstract void onClose(Player player, boolean anyViewersLeft);

    public abstract boolean onDrag(InventoryDragEvent event);

    public abstract boolean onClickIn(InventoryClickEvent event);

    public abstract boolean onClickOpen(InventoryClickEvent event);

    public abstract void onClickPost(InventoryClickEvent event);
}
