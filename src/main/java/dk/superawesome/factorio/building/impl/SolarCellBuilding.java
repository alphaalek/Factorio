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

public class SolarCellBuilding implements Building, Matcher {

    private final List<Predicate<Material>> materials = Arrays.asList(
        match(Material.DAYLIGHT_DETECTOR),
        Tag.WALL_SIGNS::isTagged
    );

    private final List<BlockVector> relatives = Arrays.asList(
        new BlockVector(),
        new BlockVector(-1, 0, 0)
    );

    @Override
    public List<BlockVector> getRelatives() {
        return relatives;
    }

    @Override
    public int getID() {
        return Profiles.SOLAR_CELL.getID();
    }

    @Override
    public List<Predicate<Material>> getMaterials() {
        return materials;
    }
}
