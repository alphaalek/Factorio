package dk.superawesome.factorio.api.events;

import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.MechanicLevel;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class MechanicRemoveEvent extends MechanicEvent {

    private static final HandlerList handlers = new HandlerList();

    public MechanicRemoveEvent(Player player, Mechanic<?> mechanic) {
        super(player, mechanic);
    }

    public int getLevelCost() {
        int cost = 0;
        if (mechanic.getLevel().lvl() > 1) {
            for (int i = 2; i <= mechanic.getLevel().lvl(); i++) {
                cost += (double) mechanic.getProfile().getLevelRegistry().get(i).get(MechanicLevel.LEVEL_COST_MARK);
            }
        }

        return cost;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
