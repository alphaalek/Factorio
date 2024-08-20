package dk.superawesome.factorio.mechanics.routes;

import org.bukkit.util.BlockVector;

public interface OutputEntry<T> {

    BlockVector getVec();

    boolean handle(T input);
}
