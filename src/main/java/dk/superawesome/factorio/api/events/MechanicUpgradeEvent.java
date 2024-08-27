package dk.superawesome.factorio.api.events;

import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class MechanicUpgradeEvent extends MechanicEvent {

    private static final HandlerList handlers = new HandlerList();

    private final int newLevel;
    private final double cost;

    public MechanicUpgradeEvent(Player player, Mechanic<?> mechanic, int newLevel, double cost) {
        super(player, mechanic);
        this.newLevel = newLevel;
        this.cost = cost;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public double getCost() {
        return cost;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
