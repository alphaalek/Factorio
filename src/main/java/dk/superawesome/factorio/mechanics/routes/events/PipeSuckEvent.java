package dk.superawesome.factorio.mechanics.routes.events;

import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

public class PipeSuckEvent extends BlockEvent {

    private static final HandlerList handlers = new HandlerList();

    private TransferCollection collection;

    public PipeSuckEvent(Block theBlock) {
        super(theBlock);
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
