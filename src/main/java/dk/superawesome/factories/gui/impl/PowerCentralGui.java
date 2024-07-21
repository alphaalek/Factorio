package dk.superawesome.factories.gui.impl;

import dk.superawesome.factories.gui.MechanicGui;
import dk.superawesome.factories.mechanics.impl.PowerCentral;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.concurrent.atomic.AtomicReference;

public class PowerCentralGui extends MechanicGui<PowerCentralGui, PowerCentral> {

    public PowerCentralGui(PowerCentral mechanic, AtomicReference<PowerCentralGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadItems() {
        super.loadItems();

        getMechanic().setEnergy(getMechanic().getEnergy() + 10);
    }

    @Override
    public void loadInputOutputItems() {

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
