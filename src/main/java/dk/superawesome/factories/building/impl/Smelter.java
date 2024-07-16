package dk.superawesome.factories.building.impl;

import dk.superawesome.factories.building.Building;
import dk.superawesome.factories.mehcanics.Profiles;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class Smelter implements Building {

    private final List<BlockVector> relatives = Arrays.asList(
            new BlockVector(),
            new BlockVector(0, 1, 0),
            new BlockVector(-1, 0, 0)
    );

    private final List<Consumer<Location>> blocks = Arrays.asList(
            l -> l.getWorld().getBlockAt(BlockUtil.getRel(l, relatives.get(0))).setType(Material.COBBLESTONE),
            l -> l.getWorld().getBlockAt(BlockUtil.getRel(l, relatives.get(1))).setType(Material.BLAST_FURNACE),
            l -> {
                Block block = l.getWorld().getBlockAt(BlockUtil.getRel(l, relatives.get(2)));
                block.setType(Material.OAK_WALL_SIGN);
                BlockUtil.setSignFacing(block, l.getWorld().getBlockAt(BlockUtil.getRel(l, relatives.get(0))), true);
            }
    );

    @Override
    public List<Consumer<Location>> getBlocks() {
        return blocks;
    }

    @Override
    public List<BlockVector> getRelatives() {
        return relatives;
    }

    @Override
    public int getID() {
        return Profiles.SMELTER.getID();
    }
}
