package dk.superawesome.factorio.mechanics.routes.events.pipe;

import dk.superawesome.factorio.mechanics.routes.events.RouteEvent;
import dk.superawesome.factorio.mechanics.routes.impl.Pipe;
import org.bukkit.event.HandlerList;

public class PipeRemoveEvent extends RouteEvent<Pipe> {

    private static final HandlerList handlers = new HandlerList();

    public PipeRemoveEvent(Pipe pipe) {
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
