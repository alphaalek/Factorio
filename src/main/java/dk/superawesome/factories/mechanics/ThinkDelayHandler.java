package dk.superawesome.factories.mechanics;

import dk.superawesome.factories.util.Tick;

public class ThinkDelayHandler {

    private int delay;
    private int lastThinkingTick = -1;

    public ThinkDelayHandler(int delay) {
        this.delay = delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public boolean ready() {
        int current = Tick.get();
        if (lastThinkingTick == -1 || current - lastThinkingTick >= delay) {
            lastThinkingTick = current;
            return true;
        }

        return false;
    }
}
