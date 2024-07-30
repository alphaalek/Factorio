package dk.superawesome.factorio.api.events;

import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public abstract class MechanicEvent extends Event implements Cancellable {

    private boolean cancelled;
    private final Mechanic<?, ?> mechanic;

    public MechanicEvent(Mechanic<?, ?> mechanic) {
        this.mechanic = mechanic;
    }

    public Mechanic<?, ?> getMechanic() {
        return mechanic;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
