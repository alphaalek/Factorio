package dk.superawesome.factories.gui;

import dk.superawesome.factories.mehcanics.Mechanic;
import dk.superawesome.factories.util.mappings.ItemMappings;

public abstract class MechanicGui<M extends Mechanic<M>> extends BaseGui {

    private final M mechanic;

    public MechanicGui(M mechanic) {
        super(BaseGui.DOUBLE_CHEST, mechanic.getProfile().getName() + " (Lvl " + mechanic.getLevel() + ")");
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
