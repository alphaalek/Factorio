package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import org.bukkit.block.Block;

public interface SignalSource {

    default boolean preSignal(AbstractRoute.Signal signal) {
        return true;
    }

    default void postSignal(AbstractRoute.Signal signal, int outputs) {

    }

    boolean handleOutput(Block block);
}
