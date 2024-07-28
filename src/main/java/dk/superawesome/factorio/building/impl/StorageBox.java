package dk.superawesome.factorio.building.impl;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.mechanics.Profiles;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BlockVector;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public class StorageBox implements Building {

    private final List<BlockVector> relatives = Arrays.asList(
            new BlockVector(),
            new BlockVector(0, 1, 0),
            new BlockVector(-1, 0, 0)
    );

    private final List<BiConsumer<Block, BlockFace>> blocks = Arrays.asList(
            (b, r) -> b.setType(Material.OAK_LOG),
            (b, r) -> {
                b.setType(Material.CHEST);
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
        return relatives;
    }

    @Override
    public int getID() {
        return Profiles.STORAGE_BOX.getID();
    }
}
