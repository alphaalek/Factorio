package dk.superawesome.factories.mehcanics;

import org.bukkit.Material;

import java.util.Arrays;

public enum Fuel {

    COAL(Material.COAL, 1f / 8f)
    ;

    public static boolean isFuel(Material mat) {
        return Arrays.stream(values()).anyMatch(fuel -> fuel.getMaterial() == mat);
    }

    public static Fuel get(Material mat) {
        return Arrays.stream(values()).filter(fuel -> fuel.getMaterial() == mat).findFirst().orElse(null);
    }

    private final Material mat;
    private final float fuelAmount;

    Fuel(Material mat, float fuelAmount) {
        this.mat = mat;
        this.fuelAmount = fuelAmount;
    }

    public Material getMaterial() {
        return mat;
    }

    public float getFuelAmount() {
        return fuelAmount;
    }
}
