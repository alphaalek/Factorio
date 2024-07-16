package dk.superawesome.factories.gui.impl;

import dk.superawesome.factories.gui.MechanicGui;
import dk.superawesome.factories.mehcanics.impl.Smelter;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.concurrent.atomic.AtomicReference;

public class SmelterGui extends MechanicGui<SmelterGui, Smelter> {

    public SmelterGui(Smelter mechanic, AtomicReference<SmelterGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void onClose() {

    }

    @Override
    public boolean onDrag(InventoryDragEvent event) {
        return false;
    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        return false;
    }

    @Override
    public boolean onClickOpen(InventoryClickEvent event) {
        return false;
    }

    @Override
    public void onClickPost(InventoryClickEvent event) {

    }
}
