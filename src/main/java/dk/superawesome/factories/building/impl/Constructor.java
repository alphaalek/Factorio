package dk.superawesome.factories.building.impl;

import dk.superawesome.factories.building.Building;
import dk.superawesome.factories.util.mappings.ItemMappings;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class Constructor implements Building {

    private final List<BlockVector> relatives = Arrays.asList(
            new BlockVector(),
            new BlockVector(0, 1, 0),
            new BlockVector(0, 0, 1)
    );

    private final List<Consumer<Location>> blocks = Arrays.asList(
            l -> l.getWorld().getBlockAt(BlockUtil.getRel(l, relatives.get(0))).setType(ItemMappings.get("oak_planks").getMaterial()),
            l -> l.getWorld().getBlockAt(BlockUtil.getRel(l, relatives.get(1))).setType(ItemMappings.get("crafting_table").getMaterial()),
            l -> {
                Block block = l.getWorld().getBlockAt(BlockUtil.getRel(l, relatives.get(2)));
                block.setType(ItemMappings.get("oak_wall_sign").getMaterial());
                BlockUtil.setSignFacing(block, l.getWorld().getBlockAt(BlockUtil.getRel(l, relatives.get(0))), true);
            }
    );

    @Override
    public int getID() {
        return 0;
    }

    @Override
    public List<Consumer<Location>> getBlocks() {
        return blocks;
    }

    @Override
    public List<BlockVector> getRelatives() {
        return relatives;
    }
}
