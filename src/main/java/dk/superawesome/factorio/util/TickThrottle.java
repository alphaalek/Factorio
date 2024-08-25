package dk.superawesome.factorio.util;

public class TickThrottle {

    private int throttledTick;

    public void throttle() {
        throttledTick = Tick.currentTick;
    }

    public boolean isThrottled() {
        return throttledTick == Tick.currentTick;
    }
}
