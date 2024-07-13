package dk.superawesome.factories.util.mappings;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public interface ItemSupplier {

    default ItemStack generateItem() {
        return generateItem(1, (short) 0);
    }

    default ItemStack generateItem(int amount) {
        return generateItem(amount, (short) 0);
    }

    Material getMaterial();

    ItemStack generateItem(int amount, short damage);
}