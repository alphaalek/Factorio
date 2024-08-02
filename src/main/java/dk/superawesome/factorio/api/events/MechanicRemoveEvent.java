package dk.superawesome.factorio.api.events;

import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class MechanicRemoveEvent extends MechanicEvent {

    private static final HandlerList handlers = new HandlerList();

    public MechanicRemoveEvent(Player player, Mechanic<?, ?> mechanic) {
        super(player, mechanic);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
