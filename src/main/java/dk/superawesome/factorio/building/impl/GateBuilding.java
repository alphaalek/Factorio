package dk.superawesome.factorio.building.impl;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Matcher;
import dk.superawesome.factorio.mechanics.Profiles;
import org.bukkit.Material;
import org.bukkit.util.BlockVector;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class GateBuilding implements Building, Matcher {

    @Override
    public List<BlockVector> getRelatives() {
        return Collections.singletonList(new BlockVector());
    }

    @Override
    public List<Predicate<Material>> getMaterials() {
        return Collections.singletonList(match(Material.PISTON));
    }

    @Override
    public int getID() {
        return Profiles.GATE.getID();
    }
}
