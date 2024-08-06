package dk.superawesome.factorio.mechanics.routes.events;

import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

public class PipePutEvent extends BlockEvent {

    private static final HandlerList handlers = new HandlerList();

    private final TransferCollection collection;
    private boolean transferred;

    public PipePutEvent(Block theBlock, TransferCollection collection) {
        super(theBlock);
        this.collection = collection;
    }

    public TransferCollection getTransfer() {
        return collection;
    }

    public boolean transferred() {
        return transferred;
    }

    public void setTransferred(boolean transferred) {
        this.transferred = transferred;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
