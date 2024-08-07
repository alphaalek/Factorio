package dk.superawesome.factorio.mechanics.routes.events;

import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.mechanics.routes.OutputEntry;
import org.bukkit.event.Event;

public abstract class RouteEvent<R extends AbstractRoute<R, ? extends OutputEntry>> extends Event {

    private final R route;

    public RouteEvent(R route) {
        this.route = route;
    }

    public R getRoute() {
        return route;
    }
}
