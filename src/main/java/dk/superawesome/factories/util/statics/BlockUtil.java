package dk.superawesome.factories.util.statics;

import dk.superawesome.factories.util.Array;
import dk.superawesome.factories.util.BlockValidator;
import dk.superawesome.factories.util.LazyInit;
import dk.superawesome.factories.util.NetProtocol;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.material.Directional;
import org.bukkit.material.Sign;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.function.Function;

@SuppressWarnings("deprecation")
public class BlockUtil {

    public static final boolean LEGACY = NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_13);

    public static BlockValidator diode = BlockValidator.fromIn(LazyInit.of(() -> Material.REPEATER), 93, 94);
    public static BlockValidator stickyPiston = BlockValidator.fromIn(LazyInit.of(() -> Material.STICKY_PISTON), 29);
    public static BlockValidator piston = BlockValidator.fromIn(LazyInit.of(() -> Material.PISTON), 33);

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

    public static Block getPointingBlock(Block block, boolean opposite) {
        if (LEGACY) {
            BlockState state = block.getState();
            if (state instanceof Directional) {
                return block.getRelative(
                        doOpposite(
                            ((Directional)state).getFacing(), opposite
                        )
                );
            }

        } else {
            BlockData data = block.getBlockData();
            if (data instanceof org.bukkit.block.data.Directional) {
                return block.getRelative(
                        doOpposite(
                            ((org.bukkit.block.data.Directional)data).getFacing(), opposite
                        )
                );
            }
        }

        return null;
    }

    public static byte getData(Block block) {
        return LEGACY ? block.getData() : (byte) 0;
    }

    public static Block getBlock(World world, BlockVector vec) {
        return world.getBlockAt(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());
    }

    public static BlockVector getVec(Block block) {
        return getVec(block.getLocation());
    }

    public static BlockVector getVec(Location loc) {
        return new BlockVector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public static Location getRel(Location loc, Vector vec) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getYaw(), loc.getPitch())
                .add(vec);
    }

    public static boolean blockEquals(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ()
                && Objects.equals(loc1.getWorld(), loc2.getWorld());
    }

    private static BlockFace doOpposite(BlockFace face, boolean opposite) {
        return opposite ? face.getOppositeFace() : face;
    }

    public static BlockFace getFaceBetween(Block from, Block to) {
        Vector vec = new Vector(
                from.getX() - to.getX(),
                from.getY() - to.getY(),
                from.getZ() - to.getZ()
        );
        if (vec.getX() != 0 || vec.getY() != 0 || vec.getZ() != 0) {
            vec.normalize();
        }

        for (BlockFace face : BlockFace.values()) {
            if (face.getDirection().equals(vec)) {
                return face;
            }
        }

        throw new IllegalStateException();
    }

    public static void setSignFacing(Block block, Block towards, boolean opposite) {
        if (BlockUtil.LEGACY) {
            Sign sign = (org.bukkit.material.Sign) block.getState();
            sign.setFacingDirection(
                    doOpposite(
                            getFaceBetween(block, towards), opposite
                    )
            );
        } else {
            org.bukkit.block.data.type.Sign sign = (org.bukkit.block.data.type.Sign) block.getBlockData();
            sign.setRotation(
                    doOpposite(
                            getFaceBetween(block, towards), opposite
                    )
            );
        }
    }
}
