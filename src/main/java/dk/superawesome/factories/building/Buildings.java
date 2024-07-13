package dk.superawesome.factories.building;

import dk.superawesome.factories.building.impl.Constructor;
import dk.superawesome.factories.mehcanics.Mechanic;
import dk.superawesome.factories.util.Array;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.util.BlockVector;

public class Buildings {

    public static Building CONSTRUCTOR;

    static {
        CONSTRUCTOR = loadBuilding(new Constructor());
    }

    private static final Array<Building> buildings = new Array<>();

    public static Building loadBuilding(Building building) {
        buildings.set(building, building);
        return building;
    }

    public static Array<Building> getBuildings() {
        return buildings;
    }

    public static boolean intersect(Location loc, Mechanic<?> mechanic) {
        for (BlockVector relVec : mechanic.getProfile().getBuilding().getRelatives()) {
            Location relLoc = BlockUtil.getRel(mechanic.getLocation(), relVec);
            if (BlockUtil.blockEquals(loc, relLoc)) {
                return true;
            }
        }

        return false;
    }
}
