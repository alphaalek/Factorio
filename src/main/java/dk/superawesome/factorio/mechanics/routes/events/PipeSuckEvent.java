package dk.superawesome.factorio.mechanics.routes.events;

import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

public class PipeSuckEvent extends BlockEvent {

    private static final HandlerList handlers = new HandlerList();

    private ItemCollection collection;

    public PipeSuckEvent(Block theBlock) {
        super(theBlock);
    }

    public ItemCollection getItems() {
        return collection;
    }

    public void setItems(ItemCollection collection) {
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
