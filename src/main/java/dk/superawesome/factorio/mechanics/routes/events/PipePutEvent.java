package dk.superawesome.factorio.mechanics.routes.events;

import dk.superawesome.factorio.mechanics.items.ItemCollection;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

public class PipePutEvent extends BlockEvent {

    private static final HandlerList handlers = new HandlerList();

    private final ItemCollection collection;

    public PipePutEvent(Block theBlock, ItemCollection collection) {
        super(theBlock);
        this.collection = collection;
    }

    public ItemCollection getItems() {
        return collection;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
