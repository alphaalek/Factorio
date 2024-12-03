package dk.superawesome.factorio.building;

import dk.superawesome.factorio.mechanics.Mechanic;

public interface BuildingCollection extends BlockCollection {

    Building get(boolean hasWallSign);

    default Building get(Mechanic<?> forMechanic) {
        return get(forMechanic.hasWallSign());
    }
}
