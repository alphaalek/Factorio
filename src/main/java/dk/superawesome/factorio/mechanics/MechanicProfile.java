package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.gui.GuiFactory;
import dk.superawesome.factorio.util.Identifiable;

public interface MechanicProfile<M extends Mechanic<M, G>, G extends BaseGui<G>> extends Identifiable {

    String getName();

    Building getBuilding();

    MechanicFactory<M> getFactory();

    GuiFactory<M, G> getGuiFactory();

    MechanicLevel.Registry getLevelRegistry();
}
