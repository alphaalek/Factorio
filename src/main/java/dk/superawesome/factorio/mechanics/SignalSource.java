package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import org.bukkit.block.Block;

public interface SignalSource {

    int FROM_POWER_CENTRAL = 0;
    int TO_POWER_CENTRAL = 1;

    default boolean preSignal(AbstractRoute.Signal signal) {
        return true;
    }

    default void postSignal(AbstractRoute.Signal signal, int outputs) {

    }

    int getContext();

    boolean handleOutput(Block block);
}