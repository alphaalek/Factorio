package dk.superawesome.factorio.building;

import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.block.Block;

public interface TopBuilding extends Building {

    default Block getSign(Mechanic<?> mechanic) {
        return BlockUtil.getRel(mechanic.getLocation(), BlockUtil.rotateVec(TOP_SIGN, DEFAULT_ROTATION, mechanic.getRotation())).getBlock();
    }

    default boolean acceptsStandingSign() {
        return true;
    }
}
