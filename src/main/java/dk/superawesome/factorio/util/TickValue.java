package dk.superawesome.factorio.util;

import java.util.function.Supplier;

public class TickValue<T> implements Supplier<T> {

    private final Supplier<T> def;
    private int lastTick;
    private T val;

    public TickValue(Supplier<T> def) {
        this.def = def;
    }

    public void set(T val) {
        this.val = val;
        lastTick = Tick.currentTick;
    }

    @Override
    public T get() {
        int tick = Tick.currentTick;
        if (tick != lastTick) {
            lastTick = tick;
            val = def != null ? def.get() : null;
        }
        return val;
    }
}
