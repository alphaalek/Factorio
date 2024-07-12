package dk.superawesome.factories.util.statics;

import dk.superawesome.factories.util.Array;
import dk.superawesome.factories.util.BlockValidator;
import dk.superawesome.factories.util.LazyInit;
import dk.superawesome.factories.util.NetProtocol;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.material.Directional;
import org.bukkit.util.BlockVector;

@SuppressWarnings("deprecation")
public class BlockUtil {

    public static final boolean LEGACY = NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_13);

    public static BlockValidator diode = BlockValidator.fromIn(LazyInit.of(() -> Material.REPEATER), 93, 94);
    public static BlockValidator stickyPiston = BlockValidator.fromIn(LazyInit.of(() -> Material.REPEATER), 29);
    public static BlockValidator piston = BlockValidator.fromIn(LazyInit.of(() -> Material.REPEATER), 33);

    public static BlockValidator anyStainedGlass = BlockValidator.fromOut(
            LazyInit.of(() -> Array.fromData(
                    Material.LEGACY_STAINED_GLASS,
                    Material.BLACK_STAINED_GLASS,
                    Material.BLUE_STAINED_GLASS,
                    Material.BROWN_STAINED_GLASS,
                    Material.CYAN_STAINED_GLASS,
                    Material.GRAY_STAINED_GLASS,
                    Material.GREEN_STAINED_GLASS,
                    Material.LIGHT_BLUE_STAINED_GLASS,
                    Material.LIGHT_GRAY_STAINED_GLASS,
                    Material.LIME_STAINED_GLASS,
                    Material.MAGENTA_STAINED_GLASS,
                    Material.ORANGE_STAINED_GLASS,
                    Material.PINK_STAINED_GLASS,
                    Material.PURPLE_STAINED_GLASS,
                    Material.RED_STAINED_GLASS,
                    Material.WHITE_STAINED_GLASS,
                    Material.YELLOW_STAINED_GLASS
            )), 95);

    public static Block getPointingBlock(Block block) {
        if (LEGACY) {
            BlockState state = block.getState();
            if (state instanceof Directional) {
                return block.getRelative(((Directional)state).getFacing());
            }

        } else {
            BlockData data = block.getBlockData();
            if (data instanceof org.bukkit.block.data.Directional) {
                return block.getRelative(((org.bukkit.block.data.Directional)data).getFacing());
            }
        }

        return null;
    }

    public static byte getData(Block block) {
        return LEGACY ? block.getData() : (byte) 0;
    }

    public static BlockVector getVec(Block block) {
        return new BlockVector(block.getX(), block.getY(), block.getZ());
    }
}
