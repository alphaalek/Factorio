package dk.superawesome.factorio.mechanics.routes.events.signal;

import dk.superawesome.factorio.mechanics.routes.events.RouteEvent;
import dk.superawesome.factorio.mechanics.routes.impl.Signal;
import org.bukkit.event.HandlerList;

public class SignalRemoveEvent extends RouteEvent<Signal> {

    private static final HandlerList handlers = new HandlerList();

    public SignalRemoveEvent(Signal signal) {
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
