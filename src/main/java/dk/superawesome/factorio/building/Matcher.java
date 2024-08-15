package dk.superawesome.factorio.building;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public interface Matcher {

    List<Predicate<Material>> getMaterials();

    default Predicate<Material> match(Material mat) {
        return mat::equals;
    }

    default Predicate<Material> matchAny(Material... mats) {
        return Arrays.stream(mats).map(this::match).reduce(Predicate::or).orElse(m -> false);
    }
}
