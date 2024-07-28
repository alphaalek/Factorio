package dk.superawesome.factorio.util;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.function.Predicate;

public interface BlockValidator extends Predicate<Material> {

    static BlockValidator from(Material... materials) {
        return material -> Arrays.stream(materials).anyMatch(m -> material == m);
    }
}
