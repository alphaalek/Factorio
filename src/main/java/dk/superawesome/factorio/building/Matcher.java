package dk.superawesome.factorio.building;

import org.bukkit.Material;

import java.util.List;
import java.util.function.Predicate;

public interface Matcher {

    List<Predicate<Material>> getMaterials();

    default Predicate<Material> match(Material mat) {
        return mat::equals;
    }
}
