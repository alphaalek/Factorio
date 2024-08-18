package dk.superawesome.factorio.mechanics.stackregistry;

import org.bukkit.Material;

public enum Fluid {

    WATER(3),

    LAVA(3),

    SNOW(3),
    ;

    private final int maxTransfer;

    Fluid(int maxTransfer) {
        this.maxTransfer = maxTransfer;
    }

    public int getMaxTransfer() {
        return maxTransfer;
    }

    public static Fluid getFluid(Material material) {
        return switch (material) {
            case POTION, WATER_BUCKET -> WATER;
            case LAVA_BUCKET -> LAVA;
            default -> null;
        };
    }
}
