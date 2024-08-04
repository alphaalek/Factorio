package dk.superawesome.factorio.api.events;

import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public abstract class MechanicEvent extends Event implements Cancellable {

    private boolean cancelled;
    private final Mechanic<?> mechanic;
    private final Player player;

    public MechanicEvent(Player player, Mechanic<?> mechanic) {
        this.mechanic = mechanic;
        this.player = player;
    }

    public Mechanic<?> getMechanic() {
        return mechanic;
    }

    public Player getPlayer() {
        return player;
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
