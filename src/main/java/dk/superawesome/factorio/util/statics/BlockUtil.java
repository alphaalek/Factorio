package dk.superawesome.factorio.util.statics;

import dk.superawesome.factorio.util.BlockValidator;
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

    public static BlockFace getRotationRelative(BlockFace def, BlockFace rel, BlockFace rot) {
        int diff = Math.abs(def.ordinal() - rel.ordinal());
        int ord = (rot.ordinal() + diff) % 4;
        return BlockFace.values()[ord];
    }

    public static void setSignFacing(Block block, Block towards, boolean opposite) {
        Sign sign = (Sign) block.getBlockData();
        sign.setRotation(
                doOpposite(
                        getFaceBetween(block, towards), opposite
                )
        );
        block.setBlockData(sign);
    }

    public static boolean isRelativeFast(Block b1, Block b2) {
        double dx = Math.abs(b1.getX() - b2.getX());
        double dy = Math.abs(b1.getY() - b2.getY());
        double dz = Math.abs(b1.getZ() - b2.getZ());

        return (dx + dy + dz) == 1;
    }

    public static boolean isDiagonalXYZFast(Block b1, Block b2) {
        return (Math.abs(b1.getX() - b2.getX()) == 1 && Math.abs(b1.getY() - b2.getY()) == 1 && Math.abs(b1.getZ() - b2.getZ()) == 1);
    }

    public static boolean isDiagonalXZFast(Block b1, Block b2) {
        return (Math.abs(b1.getX() - b2.getX()) == 1 && Math.abs(b1.getZ() - b2.getZ()) == 1 && b1.getY() == b2.getY());
    }

    public static boolean isDiagonalYFast(Block b1, Block b2) {
        double x1 = b1.getX();
        double y1 = b1.getY();
        double z1 = b1.getZ();

        double x2 = b2.getX();
        double y2 = b2.getY();
        double z2 = b2.getZ();

        return (Math.abs(x1 - x2) == 1 && Math.abs(y1 - y2) == 1 && z1 == z2)
                || (Math.abs(y1 - y2) == 1 && Math.abs(z1 - z2) == 1 && x1 == x2);
    }

    public static boolean isDiagonal2DFast(Block b1, Block b2) {
        return isDiagonalYFast(b1, b2) || isDiagonalXZFast(b1, b2);
    }

    public static boolean isDiagonal3DFast(Block b1, Block b2) {
        return isDiagonal2DFast(b1, b2) || isDiagonalXYZFast(b1, b2);
    }
}
