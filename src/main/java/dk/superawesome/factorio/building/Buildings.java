package dk.superawesome.factorio.building;

import dk.superawesome.factorio.building.impl.*;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.util.Array;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;

import java.util.ArrayList;
import java.util.List;

public class Buildings {

    public static final Building ASSEMBLER;
    public static final Building COLLECTOR;
    public static final Building COMPARATOR;
    public static final Building CONSTRUCTOR;
    public static final Building EMERALD_FORGE;
    public static final Building GATE;
    public static final Building GENERATOR;
    public static final Building SMELTER;
    public static final Building STORAGE_BOX;
    public static final Building POWER_CENTRAL;

    static {
        buildings = new Array<>();

        ASSEMBLER     = loadBuilding(new AssemblerBuilding());
        COLLECTOR     = loadBuilding(new CollectorBuilding());
        COMPARATOR    = loadBuilding(new ComparatorBuilding());
        CONSTRUCTOR   = loadBuilding(new ConstructorBuilding());
        EMERALD_FORGE = loadBuilding(new EmeraldForgeBuilding());
        GATE          = loadBuilding(new GateBuilding());
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

    private static List<Location> getLocations(Mechanic<?> mechanic) {
        List<Location> locs = new ArrayList<>();
        for (BlockVector relVec : mechanic.getProfile().getBuilding().getRelatives()) {
            Location loc = BlockUtil.getRel(mechanic.getLocation(), BlockUtil.rotateVec(relVec, Building.DEFAULT_ROTATION, mechanic.getRotation()));
            locs.add(loc);
        }

        return locs;
    }

    public static boolean intersects(Location loc, Mechanic<?> mechanic) {
        for (Location relLoc : getLocations(mechanic)) {
            if (BlockUtil.blockEquals(relLoc, loc)) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasSpaceFor(Block sign, Mechanic<?> mechanic) {
        if (mechanic.getProfile().getBuilding() instanceof Buildable) {
            for (Location relLoc : getLocations(mechanic)) {
                // check if this block can be placed in the world
                if (!relLoc.getBlock().equals(sign)
                        && !relLoc.getBlock().getLocation().equals(mechanic.getLocation())
                        && relLoc.getBlock().getType() != Material.AIR) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean checkCanBuild(Mechanic<?> mechanic) {
        Building building = mechanic.getProfile().getBuilding();
        if (building instanceof Matcher matcher) {
            int i = 0;
            // check if all the placed blocks matches the matcher
            for (Location relLoc : getLocations(mechanic)) {
                if (!matcher.getMaterials().get(i++).test(relLoc.getBlock().getType())) {
                    return false;
                }
            }
        }

        return true;
    }

    public static void build(World world, Mechanic<?> mechanic) {
        Building building = mechanic.getProfile().getBuilding();
        if (building instanceof Buildable buildable) {
            int i = 0;
            for (Location relLoc : getLocations(mechanic)) {
                buildable.getBlocks().get(i++)
                        .accept(world.getBlockAt(relLoc), mechanic.getRotation());
            }
        }
    }

    public static void remove(World world, Mechanic<?> mechanic) {
        Material signMaterial = mechanic.getLocation().getBlock().getRelative(mechanic.getRotation()).getType();
        if (mechanic.getProfile().getBuilding() instanceof Buildable) {
            for (Location relLoc : getLocations(mechanic)) {
                world.getBlockAt(relLoc).setType(Material.AIR, false); // don't apply physics
            }
        } else {
            Block sign = mechanic.getLocation().getBlock().getRelative(mechanic.getRotation());
            sign.setType(Material.AIR);
        }

        world.dropItemNaturally(mechanic.getLocation(), new ItemStack(signMaterial));
    }
}
