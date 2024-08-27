package dk.superawesome.factorio.mechanics.stackregistry;

import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

public class Fuel {

    public enum FuelType {
        WOOD(Tag.PLANKS::isTagged, Material.STICK, 1f / 2f, 3d),
        LOG(Tag.LOGS::isTagged, 1f / 5f, 4.5d),
        COAL(Material.COAL, 1f / 8f, 6.5d),
        CHARCOAL(Material.CHARCOAL, 1f / 8f, 6.5d),
        LAVA_BUCKET(Material.LAVA_BUCKET, Material.BUCKET, 1f / 125f, 11.5d),
        BLAZE_ROD(Material.BLAZE_ROD, 1f / 16f, 16d),
        COAL_BLOCK(Material.COAL_BLOCK, 1f / 72, 6.5d),
        ;

        private final Predicate<Material> tester;
        private final Material waste;
        private final double energyAmount;
        private final float fuelAmount;

        FuelType(Material mat, float fuelAmount, double energyAmount) {
            this(m -> m == mat, null, fuelAmount, energyAmount);
        }

        FuelType(Material mat, Material waste, float fuelAmount, double energyAmount) {
            this(m -> m == mat, waste, fuelAmount, energyAmount);
        }

        FuelType(Predicate<Material> tester, float fuelAmount, double energyAmount) {
            this(tester, null, fuelAmount, energyAmount);
        }

        FuelType(Predicate<Material> tester, Material waste, float fuelAmount, double energyAmount) {
            this.tester = tester;
            this.waste = waste;
            this.fuelAmount = fuelAmount;
            this.energyAmount = energyAmount;
        }
    }

    public static boolean isFuel(Material mat) {
        return Arrays.stream(FuelType.values()).anyMatch(fuel -> fuel.tester.test(mat));
    }

    public static Optional<FuelType> getType(Material mat) {
        return Arrays.stream(FuelType.values()).filter(fuel -> fuel.tester.test(mat)).findFirst();
    }

    public static Fuel getFuel(Material mat) {
        return getType(mat).map(fuelType -> new Fuel(mat, fuelType)).orElse(null);

    }

    private final FuelType type;
    private final Material material;

    public Fuel(Material material, FuelType type) {
        this.type = type;
        this.material = material;
    }

    public FuelType getType() {
        return type;
    }

    public Material getMaterial() {
        return material;
    }

    public Material getWaste() {
        return type.waste;
    }

    public float getFuelAmount() {
        return type.fuelAmount;
    }

    public double getEnergyAmount() {
        return type.energyAmount;
    }
}
