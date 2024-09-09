package dk.superawesome.factorio.util;

import com.google.common.util.concurrent.Runnables;

public interface ChainRunnable extends Runnable {

    static ChainRunnable empty() {
        return wrap(Runnables.doNothing());
    }

    static ChainRunnable wrap(Runnable runnable) {
        return runnable::run;
    }

    default ChainRunnable thenDo(Runnable runnable) {
        return () -> {
            ChainRunnable.this.run();
            runnable.run();
        };
    }
}
