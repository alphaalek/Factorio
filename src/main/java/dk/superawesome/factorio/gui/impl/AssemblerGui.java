package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.gui.GuiElement;
import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.impl.Assembler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class AssemblerGui extends MechanicGui<AssemblerGui, Assembler> {

    public AssemblerGui(Assembler mechanic, AtomicReference<AssemblerGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
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

    @Override
    public void loadInputOutputItems() {

    }

    @Override
    protected List<GuiElement> getGuiElements() {
        return null;
    }
}
