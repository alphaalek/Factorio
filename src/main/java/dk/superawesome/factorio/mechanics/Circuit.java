package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Circuit<M extends Mechanic<M>, C extends TransferCollection> extends AbstractMechanic<M> implements Container<C> {

    private static final ThreadLocal<Set<Circuit<?, ?>>> VISITED = ThreadLocal.withInitial(HashSet::new);
    private static final Map<SourceCircuitPair, Long> BLOCKED_COOLDOWN = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 2500; // 2.5 seconds cooldown

    private record SourceCircuitPair(Source source, Circuit<?, ?> circuit) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SourceCircuitPair(Source source1, Circuit<?, ?> circuit1))) return false;
            return source == source1 && circuit == circuit1;
        }

        @Override
        public int hashCode() {
            return Objects.hash(System.identityHashCode(source), System.identityHashCode(circuit));
        }
    }

    public Circuit(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public void pipePut(C collection, PipePutEvent event) {
        Source source = event.getFrom();
        SourceCircuitPair pair = new SourceCircuitPair(source, this);

        // Check cooldown first - skip if this source->circuit pair was blocked recently
        Long blockedTime = BLOCKED_COOLDOWN.get(pair);
        if (blockedTime != null && System.currentTimeMillis() - blockedTime < COOLDOWN_MS) {
            return;
        }

        Set<Circuit<?, ?>> visited = VISITED.get();
        if (visited.contains(this)) {
            // Loop detected - add cooldown only for this source->circuit pair
            BLOCKED_COOLDOWN.put(pair, System.currentTimeMillis());
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
