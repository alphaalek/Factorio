package dk.superawesome.factorio.building.impl;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Matcher;
import dk.superawesome.factorio.mechanics.Profiles;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.util.BlockVector;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class CollectorBuilding implements Building, Matcher {

    private final List<BlockVector> relatives = Arrays.asList(
            ORIGIN,
            new BlockVector(0, -1, 0),
            WALL_SIGN
    );

    private final List<Predicate<Material>> materials = Arrays.asList(
            Matcher.match(Material.CAULDRON),
            Matcher.match(Material.HOPPER),
            Tag.WALL_SIGNS::isTagged
    );

    @Override
    public List<BlockVector> getRelatives() {
        return relatives;
    }

    @Override
    public int getID() {
        return Profiles.COLLECTOR.getID();
    }

    @Override
    public List<Predicate<Material>> getMaterials() {
        return materials;
    }
}
