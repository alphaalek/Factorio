package dk.superawesome.factories.mehcanics;

import dk.superawesome.factories.building.Building;
import dk.superawesome.factories.util.Identifiable;

public interface MechanicProfile<M extends Mechanic<M>> extends Identifiable {

    String getName();

    Building getBuilding();

    MechanicFactory<M> getFactory();
}
