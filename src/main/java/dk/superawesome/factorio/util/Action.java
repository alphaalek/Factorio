package dk.superawesome.factorio.util;

import java.util.function.Consumer;

public interface Action<T> extends Consumer<T> {

    void onFinish();
}
