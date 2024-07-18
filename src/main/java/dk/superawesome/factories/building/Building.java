package dk.superawesome.factories.building;

import dk.superawesome.factories.util.Identifiable;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Furnace;
import org.bukkit.util.BlockVector;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface Building extends Identifiable {

    default void setPointingSign(Block block, BlockVector rel, BlockFace rotation) {
        BlockUtil.setSignFacing(block,
                block.getWorld().getBlockAt(BlockUtil.getRel(block.getLocation(), BlockUtil.rotateVec(rel, BlockFace.EAST, rotation))), true);
    }

    default void rotate(Block block, BlockFace rotation) {
        BlockData data = block.getBlockData();
        if (data instanceof Directional) {
            ((Directional)data).setFacing(rotation);
            block.setBlockData(data);
        }
    }

    List<BiConsumer<Block, BlockFace>> getBlocks();

    List<BlockVector> getRelatives(); // facing west
}
