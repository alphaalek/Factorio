package dk.superawesome.factories.gui;

import dk.superawesome.factories.mehcanics.Mechanic;
import dk.superawesome.factories.util.Callback;
import dk.superawesome.factories.util.mappings.ItemMappings;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public abstract class MechanicGui<G extends BaseGui<G>, M extends Mechanic<M, G>> extends BaseGui<G> {

    public static class InitCallbackHolder implements Supplier<Callback> {

        private final Callback initCallback = new Callback();

        @Override
        public Callback get() {
            return initCallback;
        }
    }

    private final M mechanic;

    public MechanicGui(M mechanic, AtomicReference<G> inUseReference, Supplier<Callback> initCallback) {
        super(initCallback, inUseReference, BaseGui.DOUBLE_CHEST, mechanic.getProfile().getName() + " (Lvl " + mechanic.getLevel() + ")");
        this.mechanic = mechanic;
    }

    @Override
    public void loadItems() {
        getInventory().setItem(52, ItemMappings.get("writable_book").generateItem());
    }

    public M getMechanic() {
        return mechanic;
    }
}
