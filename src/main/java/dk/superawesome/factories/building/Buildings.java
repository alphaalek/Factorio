package dk.superawesome.factories.building;

import dk.superawesome.factories.building.impl.Constructor;
import dk.superawesome.factories.building.impl.StorageBox;
import dk.superawesome.factories.mehcanics.Mechanic;
import dk.superawesome.factories.util.Array;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.util.BlockVector;

public class Buildings {

    public static Building CONSTRUCTOR;
    public static Building STORAGE_BOX;

    static {
        buildings = new Array<>();

        CONSTRUCTOR = loadBuilding(new Constructor());
        STORAGE_BOX = loadBuilding(new StorageBox());
    }

    private static final Array<Building> buildings;

    public static Building loadBuilding(Building building) {
        buildings.set(building, building);
        return building;
    }

    public static Array<Building> getBuildings() {
        return buildings;
    }

    public static boolean intersects(Location loc, Mechanic<?, ?> mechanic) {
        for (BlockVector relVec : mechanic.getProfile().getBuilding().getRelatives()) {
            Location relLoc = BlockUtil.getRel(mechanic.getLocation(), relVec);
            if (BlockUtil.blockEquals(loc, relLoc)) {
                return true;
            }
        }

        return false;
    }
}
