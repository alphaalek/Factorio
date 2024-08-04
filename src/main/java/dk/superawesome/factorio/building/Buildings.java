package dk.superawesome.factorio.building;

import dk.superawesome.factorio.building.impl.*;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.util.Array;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;

import java.util.ArrayList;
import java.util.List;

public class Buildings {

    public static Building ASSEMBLER;
    public static Building CONSTRUCTOR;
    public static Building EMERALD_FORGE;
    public static Building GENERATOR;
    public static Building SMELTER;
    public static Building STORAGE_BOX;
    public static Building POWER_CENTRAL;

    static {
        buildings = new Array<>();

        ASSEMBLER     = loadBuilding(new AssemblerBuilding());
        CONSTRUCTOR   = loadBuilding(new ConstructorBuilding());
        EMERALD_FORGE = loadBuilding(new EmeraldForgeBuilding());
        GENERATOR     = loadBuilding(new GeneratorBuilding());
        SMELTER       = loadBuilding(new SmelterBuilding());
        STORAGE_BOX   = loadBuilding(new StorageBoxBuilding());
        POWER_CENTRAL = loadBuilding(new PowerCentralBuilding());
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

    public static void remove(World world, Mechanic<?, ?> mechanic) {
        for (Location relLoc : getLocations(mechanic)) {
            world.getBlockAt(relLoc).setType(Material.AIR, false); // don't apply physics
        }
        world.dropItemNaturally(mechanic.getLocation(), new ItemStack(Material.OAK_SIGN));
    }
}
