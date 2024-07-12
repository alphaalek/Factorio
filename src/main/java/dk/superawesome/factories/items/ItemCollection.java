package dk.superawesome.factories.items;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface ItemCollection {

    static ItemCollection from(Inventory inventory) {
        return new ItemCollection() {
            @Override
            public boolean has(Material material, int amount) {
                return false;
            }

            @Override
            public List<ItemStack> get(Material material, int amount) {
                return null;
            }
        };
    }

    boolean has(Material material, int amount);

    List<ItemStack> get(Material material, int amount);
}
