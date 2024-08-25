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

public class CauldronBuilding implements Building, Matcher {

    private final List<BlockVector> relatives = Arrays.asList(
            new BlockVector(),
            new BlockVector(-1, 0, 0)
    );

    private final List<Predicate<Material>> materials = Arrays.asList(
            matchAny(Material.CAULDRON, Material.WATER_CAULDRON, Material.LAVA_CAULDRON, Material.POWDER_SNOW_CAULDRON),
            Tag.WALL_SIGNS::isTagged
    );

    @Override
    public List<BlockVector> getRelatives() {
        return relatives;
    }

    @Override
    public List<Predicate<Material>> getMaterials() {
        return materials;
    }

    @Override
    public int getID() {
        return Profiles.CAULDRON.getID();
    }
}
