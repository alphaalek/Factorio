package dk.superawesome.factories.gui;

import dk.superawesome.factories.mechanics.Mechanic;

import java.util.concurrent.atomic.AtomicReference;

public interface GuiFactory<M extends Mechanic<M, G>, G extends BaseGui<G>> {

    G create(M mechanic, AtomicReference<G> inUseReference);
}
