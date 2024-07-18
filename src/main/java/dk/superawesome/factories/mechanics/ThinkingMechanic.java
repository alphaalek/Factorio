package dk.superawesome.factories.mechanics;

import dk.superawesome.factories.util.TickThrottle;

public interface ThinkingMechanic {

    ThinkDelayHandler getDelayHandler();

    void think();

    TickThrottle getTickThrottle();
}
