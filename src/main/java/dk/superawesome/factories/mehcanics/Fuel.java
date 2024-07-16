package dk.superawesome.factories.mehcanics;

import org.bukkit.Material;

import java.util.Arrays;

public enum Fuel {

    COAL(Material.COAL)
    ;

    public static boolean isFuel(Material mat) {
        return Arrays.stream(values()).anyMatch(fuel -> fuel.getMaterial() == mat);
    }

    public static Fuel get(Material mat) {
        return Arrays.stream(values()).filter(fuel -> fuel.getMaterial() == mat).findFirst().orElse(null);
    }

    private final Material mat;

    Fuel(Material mat) {
        this.mat = mat;
    }

    public Material getMaterial() {
        return mat;
    }
}
