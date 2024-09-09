package dk.superawesome.factorio.mechanics.routes.events.pipe;

import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

public class PipeSuckEvent extends BlockEvent {

    private static final HandlerList handlers = new HandlerList();

    private TransferCollection collection;
    private final Location location;

    public PipeSuckEvent(Block theBlock, Location location) {
        super(theBlock);
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public TransferCollection getTransfer() {
        return collection;
    }

    public void setTransfer(TransferCollection collection) {
        this.collection = collection;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
