package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Circuit<M extends Mechanic<M>, C extends TransferCollection> extends AbstractMechanic<M> implements Container<C> {

    private static final ThreadLocal<Set<Circuit<?, ?>>> VISITED = ThreadLocal.withInitial(HashSet::new);
    private static final Map<Circuit<?, ?>, Long> BLOCKED_COOLDOWN = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 2500; // 2.5 seconds cooldown

    public Circuit(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public void pipePut(C collection, PipePutEvent event) {
        // Check cooldown first - skip if this circuit was blocked recently
        Long blockedTime = BLOCKED_COOLDOWN.get(this);
        if (blockedTime != null && System.currentTimeMillis() - blockedTime < COOLDOWN_MS) {
            return;
        }

        Set<Circuit<?, ?>> visited = VISITED.get();
        if (visited.contains(this)) {
            // Loop detected - add to cooldown
            BLOCKED_COOLDOWN.put(this, System.currentTimeMillis());
            return;
        }

        visited.add(this);
        try {
            if (pipePut(collection)) {
                event.setTransferred(true);
            }
        } finally {
            visited.remove(this);
        }
    }

    public abstract boolean pipePut(C collection);
}
