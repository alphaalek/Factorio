package dk.superawesome.factories.building.impl;

import dk.superawesome.factories.building.Building;
import dk.superawesome.factories.mehcanics.Profiles;
import dk.superawesome.factories.util.mappings.ItemMappings;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class StorageBox implements Building {

    private final List<BlockVector> relatives = Arrays.asList(
            new BlockVector(),
            new BlockVector(0, 1, 0),
            new BlockVector(-1, 0, 0)
    );

    private final List<Consumer<Location>> blocks = Arrays.asList(
            l -> l.getWorld().getBlockAt(BlockUtil.getRel(l, relatives.get(0))).setType(ItemMappings.get("oak_log").getMaterial()),
            l -> l.getWorld().getBlockAt(BlockUtil.getRel(l, relatives.get(1))).setType(ItemMappings.get("chest").getMaterial()),
            l -> {
                Block block = l.getWorld().getBlockAt(BlockUtil.getRel(l, relatives.get(2)));
                block.setType(ItemMappings.get("oak_wall_sign").getMaterial());
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
        return Profiles.STORAGE_BOX.getID();
    }
}
