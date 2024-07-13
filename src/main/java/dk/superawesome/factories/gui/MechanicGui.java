package dk.superawesome.factories.gui;

import dk.superawesome.factories.mehcanics.Mechanic;
import dk.superawesome.factories.util.Callback;
import dk.superawesome.factories.util.mappings.ItemMappings;

import java.util.function.Supplier;

public abstract class MechanicGui<M extends Mechanic<M>> extends BaseGui {

    public static class InitCallbackHolder implements Supplier<Callback> {

        private final Callback initCallback = new Callback();

        @Override
        public Callback get() {
            return initCallback;
        }
    }

    private final M mechanic;

    public MechanicGui(M mechanic, Supplier<Callback> initCallback) {
        super(initCallback, BaseGui.DOUBLE_CHEST, mechanic.getProfile().getName() + " (Lvl " + mechanic.getLevel() + ")");
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
