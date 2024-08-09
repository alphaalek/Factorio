package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.util.TickThrottle;

public interface ThinkingMechanic {

    DelayHandler getThinkDelayHandler();

    void think();

    TickThrottle getTickThrottle();
}
