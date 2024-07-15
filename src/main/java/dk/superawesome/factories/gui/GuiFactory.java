package dk.superawesome.factories.gui;

import dk.superawesome.factories.mehcanics.Mechanic;

import java.util.concurrent.atomic.AtomicReference;

public interface GuiFactory<M extends Mechanic<M>, G extends BaseGui> {

    G create(M mechanic, AtomicReference<BaseGui> inUseReference);
}
