package dk.superawesome.factorio.building;

import dk.superawesome.factorio.util.Identifiable;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.util.BlockVector;

import java.util.List;
import java.util.function.BiConsumer;

public interface Building extends Identifiable {

    BlockFace DEFAULT_ROTATION = BlockFace.WEST;

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
