package dk.superawesome.factorio.mechanics.stackregistry;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Optional;

public enum Volume {

    BOTTLE(Material.GLASS_BOTTLE, 1),

    BUCKET(Material.BUCKET, 3)
    ;

    private final Material mat;
    private final int fluidRequires;

    Volume(Material mat, int fluidRequires) {
        this.mat = mat;
        this.fluidRequires = fluidRequires;
    }

    public Material getMat() {
        return mat;
    }

    public int getFluidRequires() {
        return fluidRequires;
    }

    public static Optional<Volume> getType(Material type) {
        return Arrays.stream(values()).filter(volume -> volume.getMat().equals(type)).findFirst();
    }
}
