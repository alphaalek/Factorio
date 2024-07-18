package dk.superawesome.factories.mechanics;

import dk.superawesome.factories.gui.BaseGui;

public interface ThinkingMechanic<M extends Mechanic<M, G>, G extends BaseGui<G>> extends Mechanic<M, G> {

    ThinkDelayHandler getDelayHandler();

    void think();
}
