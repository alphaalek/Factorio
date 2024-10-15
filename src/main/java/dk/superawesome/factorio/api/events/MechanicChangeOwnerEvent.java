package dk.superawesome.factorio.api.events;

import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class MechanicChangeOwnerEvent extends MechanicEvent {

    private static final HandlerList handlers = new HandlerList();

    private final UUID newOwner;

    public MechanicChangeOwnerEvent(Player player, Mechanic<?> mechanic, UUID newOwner) {
        super(player, mechanic);
        this.newOwner = newOwner;
    }

    public UUID getNewOwner() {
        return newOwner;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
