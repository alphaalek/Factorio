package dk.superawesome.factorio.mechanics.routes.events.signal;

import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.mechanics.routes.events.RouteEvent;
import org.bukkit.event.HandlerList;

public class SignalRemoveEvent extends RouteEvent<AbstractRoute.Signal> {

    private static final HandlerList handlers = new HandlerList();

    public SignalRemoveEvent(AbstractRoute.Signal signal) {
        super(signal);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}