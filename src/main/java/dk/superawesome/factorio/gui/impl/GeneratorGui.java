package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.impl.Generator;

import java.util.concurrent.atomic.AtomicReference;

public class GeneratorGui extends MechanicGui<GeneratorGui, Generator> {

    public GeneratorGui(Generator mechanic, AtomicReference<GeneratorGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
    }

    @Override
    public void loadInputOutputItems() {

    }
}
