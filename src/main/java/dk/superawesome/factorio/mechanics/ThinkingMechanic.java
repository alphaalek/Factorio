package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.util.TickThrottle;

public interface ThinkingMechanic {

    ThinkDelayHandler getDelayHandler();

    void think();

    TickThrottle getTickThrottle();
}
