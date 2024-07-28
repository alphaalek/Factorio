package dk.superawesome.factorio.util;

import java.util.function.Supplier;

public class LazyInit<T> implements Supplier<T> {

    public static <T> LazyInit<T> of(Supplier<T> function) {
        return new LazyInit<>(function);
    }

    private T val;
    private final Supplier<T> function;

    public LazyInit(Supplier<T> function) {
        this.function = function;
    }

    public T get() {
        return val == null ? (val = function.get()) : val;
    }
}
