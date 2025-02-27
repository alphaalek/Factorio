package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.util.Callback;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class BaseGuiAdapter<G extends BaseGui<G>> extends BaseGui<G> {

    private final boolean cancel;

    public BaseGuiAdapter(Supplier<Callback> initCallback, AtomicReference<G> inUseReference, int size, String title, boolean cancel) {
        super(initCallback, inUseReference, size, title);
        this.cancel = cancel;
    }

    public BaseGuiAdapter(Supplier<Callback> initCallback, AtomicReference<G> inUseReference, InventoryType type, String title, boolean cancel) {
        super(initCallback, inUseReference, type, title);
        this.cancel = cancel;
    }

    @Override
    public void loadItems() {

    }

    @Override
    public void onClose(Player player, boolean anyViewersLeft) {

    }

    @Override
    public boolean onDrag(InventoryDragEvent event) {
        if (event.getRawSlots().stream().anyMatch(i -> event.getView().getInventory(i).equals(getInventory()))) {
            return cancel;
        }

        return false;
    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        return cancel;
    }

    @Override
    public boolean onClickOpen(InventoryClickEvent event) {
        return cancel && movedFromOtherInventory(event);
    }

    @Override
    public void onClickPost(InventoryClickEvent event) {

    }
}
