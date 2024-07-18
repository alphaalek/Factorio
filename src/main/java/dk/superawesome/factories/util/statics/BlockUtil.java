package dk.superawesome.factories.util.statics;

import dk.superawesome.factories.util.BlockValidator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Sign;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.util.Objects;

@SuppressWarnings("deprecation")
public class BlockUtil {

    public static BlockValidator diode = BlockValidator.from(Material.REPEATER);
    public static BlockValidator stickyPiston = BlockValidator.from(Material.STICKY_PISTON);
    public static BlockValidator piston = BlockValidator.from(Material.PISTON);

    public static BlockValidator anyStainedGlass = BlockValidator.from(
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
            Material.YELLOW_STAINED_GLASS);

    public static Block getPointingBlock(Block block, boolean opposite) {
        BlockData data = block.getBlockData();
        if (data instanceof org.bukkit.block.data.Directional) {
            return block.getRelative(
                    doOpposite(
                        ((org.bukkit.block.data.Directional)data).getFacing(), opposite
                    )
            );
        }

        return null;
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

    public static Vector rotateVec(Vector vec, BlockFace ori, BlockFace rot) {
        int angle = getXZAngle(ori.getDirection(), rot.getDirection());

        Vector newVec = vec.clone();
        // apply the rotation based on the angle between the original and rotated directions
        switch (angle) {
            case 90:
                newVec.setX(vec.getZ());
                newVec.setZ(-vec.getX());
                break;
            case -90:
                newVec.setX(-vec.getZ());
                newVec.setZ(vec.getX());
                break;
            case 180:
                newVec.setX(-vec.getX());
                newVec.setZ(-vec.getZ());
                break;
            default:
                break; // no rotation needed
        }

        return newVec;
    }

    private static int getXZAngle(Vector oriVec, Vector rotVec) {
        int oriX = oriVec.getBlockX();
        int oriZ = oriVec.getBlockZ();
        int rotX = rotVec.getBlockX();
        int rotZ = rotVec.getBlockZ();

        if (oriX == rotX && oriZ == rotZ) {
            return 0; // no rotation
        } else if (oriX == -rotZ && oriZ == rotX) {
            return 90; // 90 degrees clockwise
        } else if (oriX == rotZ && oriZ == -rotX) {
            return -90; // 90 degrees counterclockwise
        } else if (oriX == -rotX && oriZ == -rotZ) {
            return 180; // 180 degrees
        } else {
            throw new IllegalArgumentException();
        }
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
        Vector vec = new Vector(from.getX() - to.getX(), from.getY() - to.getY(), from.getZ() - to.getZ());
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
        Sign sign = (Sign) block.getBlockData();
        sign.setRotation(
                doOpposite(
                        getFaceBetween(block, towards), opposite
                )
        );
    }
}
