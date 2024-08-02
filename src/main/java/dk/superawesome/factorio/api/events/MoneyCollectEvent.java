package dk.superawesome.factorio.api.events;

import dk.superawesome.factorio.mechanics.transfer.MoneyContainer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MoneyCollectEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled;

    private final Player player;
    private final double amount;
    private final MoneyContainer container;

    public MoneyCollectEvent(Player player, double amount, MoneyContainer container) {
        this.player = player;
        this.amount = amount;
        this.container = container;
    }

    public Player getPlayer() {
        return player;
    }

    public double getAmount() {
        return amount;
    }

    public MoneyContainer getContainer() {
        return container;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
