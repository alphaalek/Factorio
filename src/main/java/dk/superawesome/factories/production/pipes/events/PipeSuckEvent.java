package dk.superawesome.factories.production.pipes.events;

import dk.superawesome.factories.items.ItemCollection;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
