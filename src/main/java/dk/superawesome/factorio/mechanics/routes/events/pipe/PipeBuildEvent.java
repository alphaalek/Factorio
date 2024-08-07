package dk.superawesome.factorio.mechanics.routes.events.pipe;

import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.mechanics.routes.events.RouteEvent;
import org.bukkit.event.HandlerList;

public class PipeBuildEvent extends RouteEvent<AbstractRoute.Pipe> {

    private static final HandlerList handlers = new HandlerList();

    public PipeBuildEvent(AbstractRoute.Pipe pipe) {
        super(pipe);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
