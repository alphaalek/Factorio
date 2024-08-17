package dk.superawesome.factorio.building;

import dk.superawesome.factorio.util.Identifiable;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.util.BlockVector;

import java.util.Arrays;
import java.util.List;

public interface Building extends Identifiable {

    List<BlockVector> DEFAULT_RELATIVES = Arrays.asList(
            new BlockVector(),
            new BlockVector(0, 1, 0),
            new BlockVector(-1, 0, 0)
    );

    BlockFace DEFAULT_ROTATION = BlockFace.WEST;

    default void rotate(Block block, BlockFace rotation) {
        BlockData data = block.getBlockData();
        if (data instanceof Directional) {
            ((Directional)data).setFacing(rotation);
            block.setBlockData(data);
        }
    }

    List<BlockVector> getRelatives(); // facing west
}
