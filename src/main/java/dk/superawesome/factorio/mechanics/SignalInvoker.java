package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.mechanics.impl.behaviour.PowerCentral;
import dk.superawesome.factorio.mechanics.routes.AbstractRoute;

import java.util.Set;

public interface SignalInvoker {

    boolean invoke(PowerCentral source);
}
