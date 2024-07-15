package dk.superawesome.factories.util;

public class TickThrottle {

    private int throttledTick;

    public void throttle() {
        throttledTick = Tick.get();
    }

    public boolean isThrottled() {
        return throttledTick == Tick.get();
    }
}
