package dk.superawesome.factorio.util;

public class TickThrottle {

    private int throttledTick;

    public void throttle() {
        throttledTick = Tick.get();
    }

    public boolean isThrottled() {
        return throttledTick == Tick.get();
    }
}
