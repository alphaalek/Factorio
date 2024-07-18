package dk.superawesome.factories.mechanics;

import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

public class Fuel {

    public enum FuelType {
        COAL(Material.COAL, 1f / 8f),
        CHARCOAL(Material.CHARCOAL, 1f / 8f),
        BLAZE_ROD(Material.BLAZE_ROD, 1f / 16f),
        COAL_BLOCK(Material.COAL_BLOCK, 1f / 72),
        LAVA_BUCKET(Material.LAVA_BUCKET, 1f / 100f),
        WOOD(Tag.PLANKS::isTagged, 1f / 2f),
        LOG(Tag.LOGS::isTagged, 1f / 5f),
        ;

        private final Predicate<Material> tester;
        private final float fuelAmount;

        FuelType(Material mat, float fuelAmount) {
            this(m -> m == mat, fuelAmount);
        }

        FuelType(Predicate<Material> tester, float fuelAmount) {
            this.tester = tester;
            this.fuelAmount = fuelAmount;
        }
    }

    public static boolean isFuel(Material mat) {
        return Arrays.stream(FuelType.values()).anyMatch(fuel -> fuel.tester.test(mat));
    }

    public static Optional<FuelType> getType(Material mat) {
        return Arrays.stream(FuelType.values()).filter(fuel -> fuel.tester.test(mat)).findFirst();
    }

    public static Fuel get(Material mat) {
        return getType(mat).map(fuelType -> new Fuel(mat, fuelType)).orElse(null);

    }

    private final FuelType type;
    private final Material material;

    public Fuel(Material material, FuelType type) {
        this.type = type;
        this.material = material;
    }

    public Material getMaterial() {
        return material;
    }

    public float getFuelAmount() {
        return type.fuelAmount;
    }
}
