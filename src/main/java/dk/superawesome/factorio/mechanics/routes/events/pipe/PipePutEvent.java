package dk.superawesome.factorio.mechanics.routes.events.pipe;

import dk.superawesome.factorio.mechanics.Source;
import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

import java.util.Set;

public class PipePutEvent extends BlockEvent {

    private static final HandlerList handlers = new HandlerList();

    private final TransferCollection collection;
    private final Source from;
    private boolean transferred;

    public PipePutEvent(Block theBlock, TransferCollection collection, Source from) {
        super(theBlock);
        this.collection = collection;
        this.from = from;
    }

    public TransferCollection getTransfer() {
        return collection;
    }

    public Source getFrom() {
        return from;
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
