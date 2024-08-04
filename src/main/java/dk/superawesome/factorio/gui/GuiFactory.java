package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.mechanics.Mechanic;

import java.util.concurrent.atomic.AtomicReference;

public interface GuiFactory<M extends Mechanic<M>, G extends BaseGui<G>> {

    G create(M mechanic, AtomicReference<G> inUseReference);
}
