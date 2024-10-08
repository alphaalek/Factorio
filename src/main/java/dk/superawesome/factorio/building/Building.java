package dk.superawesome.factorio.building;

import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BlockVector;

import java.util.Arrays;
import java.util.List;

public interface Building extends BlockCollection {

    BlockVector WALL_SIGN = new BlockVector(-1, 0, 0);
    BlockVector TOP_SIGN = new BlockVector(0, 1, 0);

    BlockVector ORIGIN = new BlockVector(0, 0, 0);

    List<BlockVector> DEFAULT_MECHANIC_RELATIVES = Arrays.asList(
            ORIGIN,
            new BlockVector(0, 1, 0),
            WALL_SIGN
    );

   List<BlockVector> SMALL_MECHANIC_RELATIVES = Arrays.asList(
           ORIGIN,
           WALL_SIGN
    );

    BlockFace DEFAULT_ROTATION = BlockFace.WEST;

    default Block getSign(Mechanic<?> mechanic) {
        return BlockUtil.getRel(mechanic.getLocation(), BlockUtil.rotateVec(WALL_SIGN, DEFAULT_ROTATION, mechanic.getRotation())).getBlock();
    }

    default boolean deniesStandingSign() {
        return true;
    }

    List<BlockVector> getRelatives(); // facing default rotation
}
