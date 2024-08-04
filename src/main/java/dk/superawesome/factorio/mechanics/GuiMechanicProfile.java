package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.gui.GuiFactory;

public interface GuiMechanicProfile<M extends Mechanic<M>> extends MechanicProfile<M> {

    <G extends BaseGui<G>> GuiFactory<M, G> getGuiFactory();
}
