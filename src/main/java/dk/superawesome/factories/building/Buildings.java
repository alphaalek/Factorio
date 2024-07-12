package dk.superawesome.factories.building;

import dk.superawesome.factories.building.impl.Constructor;
import dk.superawesome.factories.util.Array;

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
}
