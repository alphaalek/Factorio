package dk.superawesome.factorio.api.events;

import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.List;

public class MechanicMoveEvent extends MechanicEvent {

    private static final HandlerList handlers = new HandlerList();

    private final Location to;
    private final BlockFace rotation;

    public MechanicMoveEvent(Player player, Mechanic<?> mechanic, Location to, BlockFace rotation) {
        super(player, mechanic);
        this.to = to;
        this.rotation = rotation;
    }

    public Location getTo() {
        return to;
    }

    public BlockFace getRotation() {
        return rotation;
    }

    public List<Location> getLocations() {
        return Buildings.getLocations(mechanic.getBuilding(), to, rotation);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
