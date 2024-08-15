package dk.superawesome.factorio.mechanics.transfer;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Predicate;

public interface FluidCollection extends TransferCollection {

    int CAPACITY_MARK = 0;

    List<ItemStack> take(int amount);

    boolean hasFluid(FluidType fluidType);

    boolean hasFluid(Predicate<FluidType> fluidType);

    enum FluidType {
        WATER,
        LAVA;

        public Material getMaterial() {
            return switch (this) {
                case WATER -> Material.WATER;
                case LAVA -> Material.LAVA;
            };
        }
    }
}
