package dk.superawesome.factorio.gui.impl;

import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.mechanics.impl.Generator;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeneratorGui extends MechanicGui<GeneratorGui, Generator> {

    public GeneratorGui(Generator mechanic, AtomicReference<GeneratorGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadItems() {
        super.loadItems();
    }

    @Override
    public void loadInputOutputItems() {

    }

    public void updateFuelState() {
        updateFuelState(Stream.of(0, 9, 18, 27, 36, 45).sorted(Collections.reverseOrder()).collect(Collectors.toList()));
    }

    public void updateRemovedFuel(int amount) {

    }
}
