package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.mechanics.impl.power.PowerCentral;

public interface SignalInvoker {

    boolean invoke(PowerCentral source);
}
