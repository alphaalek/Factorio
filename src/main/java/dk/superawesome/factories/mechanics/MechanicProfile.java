package dk.superawesome.factories.mechanics;

import dk.superawesome.factories.building.Building;
import dk.superawesome.factories.gui.BaseGui;
import dk.superawesome.factories.gui.GuiFactory;
import dk.superawesome.factories.util.Identifiable;

public interface MechanicProfile<M extends Mechanic<M, G>, G extends BaseGui<G>> extends Identifiable {

    String getName();

    Building getBuilding();

    MechanicFactory<M> getFactory();

    GuiFactory<M, G> getGuiFactory();
}
