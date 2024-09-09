package dk.superawesome.factorio.building.impl;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.BuildingCollection;
import dk.superawesome.factorio.building.Matcher;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.Profiles;
import dk.superawesome.factorio.mechanics.impl.circuits.Gate;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class GateBuilding implements BuildingCollection {

    private final Side side = new Side();
    private final Top top = new Top();

    @Override
    public Building get(Mechanic<?> forMechanic) {
        return forMechanic.hasWallSign() ? side : top;
    }

    static class Side implements Building, Matcher {

        private final List<Predicate<Material>> materials = Arrays.asList(
                Matcher.match(Material.CAULDRON),
                Tag.WALL_SIGNS::isTagged
        );

        @Override
        public List<BlockVector> getRelatives() {
            return SMALL_MECHANIC_RELATIVES;
        }

        @Override
        public List<Predicate<Material>> getMaterials() {
            return materials;
        }

        @Override
        public int getID() {
            return Profiles.GATE.getID();
        }
    }

    static class Top implements Building, Matcher {

        private final List<BlockVector> relatives = Arrays.asList(
                ORIGIN,
                TOP_SIGN
        );

        private final List<Predicate<Material>> materials = Arrays.asList(
                Matcher.match(Material.CAULDRON),
                Tag.STANDING_SIGNS::isTagged
        );

        @Override
        public Block getSign(Mechanic<?> mechanic) {
            return BlockUtil.getRel(mechanic.getLocation(), BlockUtil.rotateVec(TOP_SIGN, DEFAULT_ROTATION, mechanic.getRotation())).getBlock();
        }

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
            return Profiles.GATE.getID();
        }
    }

    @Override
    public int getID() {
        return Profiles.GATE.getID();
    }
}
