package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.impl.behaviour.LiquidTank;

import java.util.concurrent.atomic.AtomicReference;

public class LiquidTankGui extends MechanicGui<LiquidTankGui, LiquidTank>{

    public LiquidTankGui(LiquidTank mechanic, AtomicReference<LiquidTankGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadInputOutputItems() {

    }
}
