package dk.superawesome.factories.mehcanics;

import dk.superawesome.factories.util.Identifiable;

public interface MechanicProfile<M extends Mechanic<M>> extends Identifiable {

    String getName();

    MechanicFactory<M> getFactory();
}
