package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.util.Callback;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class BaseGuiAdapter<G extends BaseGui<G>> extends BaseGui<G> {

    private final boolean cancel;

    public BaseGuiAdapter(Supplier<Callback> initCallback, AtomicReference<G> inUseReference, int size, String title, boolean cancel) {
        super(initCallback, inUseReference, size, title);
        this.cancel = cancel;
    }

    @Override
    public void loadItems() {

    }

    @Override
    public void onClose(Player player) {

    }

    @Override
    public boolean onDrag(InventoryDragEvent event) {
        return cancel;
    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        return cancel;
    }

    @Override
    public boolean onClickOpen(InventoryClickEvent event) {
        return cancel;
    }

    @Override
    public void onClickPost(InventoryClickEvent event) {

    }
}
