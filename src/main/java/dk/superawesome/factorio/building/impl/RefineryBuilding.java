package dk.superawesome.factorio.building.impl;

import dk.superawesome.factorio.building.Buildable;
import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.mechanics.Profiles;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BlockVector;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public class RefineryBuilding implements Building, Buildable {

    private final List<BiConsumer<Block, BlockFace>> blocks = Arrays.asList(
            (b, r) -> b.setType(Material.BEE_NEST),
            (b, r) -> {
                b.setType(Material.BREWING_STAND);
                rotate(b, r);
            },
            (b, r) -> {} /* setPointingSign(b, relatives.get(0), r) */
    );

    @Override
    public List<BiConsumer<Block, BlockFace>> getBlocks() {
        return blocks;
    }

    @Override
    public List<BlockVector> getRelatives() {
        return DEFAULT_RELATIVES;
    }

    @Override
    public int getID() {
        return Profiles.STORAGE_BOX.getID();
    }
}
