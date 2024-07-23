package dk.superawesome.factories.building;

import dk.superawesome.factories.building.impl.Constructor;
import dk.superawesome.factories.building.impl.PowerCentral;
import dk.superawesome.factories.building.impl.Smelter;
import dk.superawesome.factories.building.impl.StorageBox;
import dk.superawesome.factories.mechanics.Mechanic;
import dk.superawesome.factories.util.Array;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.util.BlockVector;

import java.util.ArrayList;
import java.util.List;

public class Buildings {

    public static Building CONSTRUCTOR;
    public static Building SMELTER;
    public static Building STORAGE_BOX;
    public static Building POWER_CENTRAL;

    static {
        buildings = new Array<>();

        CONSTRUCTOR   = loadBuilding(new Constructor());
        SMELTER       = loadBuilding(new Smelter());
        STORAGE_BOX   = loadBuilding(new StorageBox());
        POWER_CENTRAL = loadBuilding(new PowerCentral());
    }

    private static final Array<Building> buildings;

    public static Building loadBuilding(Building building) {
        buildings.set(building, building);
        return building;
    }

    public static Array<Building> getBuildings() {
        return buildings;
    }

    private static List<Location> getLocations(Mechanic<?, ?> mechanic) {
        List<Location> locs = new ArrayList<>();
        for (BlockVector relVec : mechanic.getProfile().getBuilding().getRelatives()) {
            Location loc = BlockUtil.getRel(mechanic.getLocation(), BlockUtil.rotateVec(relVec, Building.DEFAULT_ROTATION, mechanic.getRotation()));
            locs.add(loc);
        }

        return locs;
    }

    public static boolean intersects(Location loc, Mechanic<?, ?> mechanic) {
        for (Location relLoc : getLocations(mechanic)) {
            if (BlockUtil.blockEquals(relLoc, loc)) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasSpaceFor(World world, Block sign, Mechanic<?, ?> mechanic) {
        for (Location relLoc : getLocations(mechanic)) {
            Block block = world.getBlockAt(relLoc);
            // check if this block can be placed in the world
            if (!block.equals(sign)
                    && !block.getLocation().equals(mechanic.getLocation())
                    && block.getType() != Material.AIR) {
                return false;
            }
        }

        return true;
    }

    public static void build(World world, Mechanic<?, ?> mechanic) {
        int i = 0;
        for (Location relLoc : getLocations(mechanic)) {
            mechanic.getProfile().getBuilding().getBlocks().get(i++)
                    .accept(world.getBlockAt(relLoc), mechanic.getRotation());
        }
    }
}
