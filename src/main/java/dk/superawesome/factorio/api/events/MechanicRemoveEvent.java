package dk.superawesome.factorio.api.events;

import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.event.HandlerList;

public class MechanicRemoveEvent extends MechanicEvent {

    private static final HandlerList handlers = new HandlerList();

    public MechanicRemoveEvent(Mechanic<?, ?> mechanic) {
        super(mechanic);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
