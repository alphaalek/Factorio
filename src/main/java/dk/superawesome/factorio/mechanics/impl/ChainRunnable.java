package dk.superawesome.factorio.mechanics.impl;

public interface ChainRunnable extends Runnable {

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