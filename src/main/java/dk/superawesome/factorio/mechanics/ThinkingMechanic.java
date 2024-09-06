package dk.superawesome.factorio.mechanics;

public interface ThinkingMechanic {

    DelayHandler getThinkDelayHandler();

    void think();

}
