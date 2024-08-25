package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.util.Tick;

public class DelayHandler {

    public static DelayHandler NO_DELAY = new DelayHandler(0);

    private int delay;
    private int lastCheckTick = -1;

    public DelayHandler(int delay) {
        this.delay = delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public boolean ready() {
        int current = Tick.currentTick;
        if (lastCheckTick == -1 || current - lastCheckTick >= delay) {
            lastCheckTick = current;
            return true;
        }

        return false;
    }
}
