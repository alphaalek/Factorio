package dk.superawesome.factorio.api.events;

import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MechanicLoadEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Mechanic<?> mechanic;

    public MechanicLoadEvent(Mechanic<?> mechanic) {
        this.mechanic = mechanic;
    }

    public Mechanic<?> getMechanic() {
        return mechanic;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
