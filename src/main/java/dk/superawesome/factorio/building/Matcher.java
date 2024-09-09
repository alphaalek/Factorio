package dk.superawesome.factorio.building;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public interface Matcher {

    List<Predicate<Material>> getMaterials();

    static Predicate<Material> match(Material mat) {
        return mat::equals;
    }

    static Predicate<Material> matchAny(Material... mats) {
        return Arrays.stream(mats).map(Matcher::match).reduce(Predicate::or).orElse(m -> false);
    }
}
