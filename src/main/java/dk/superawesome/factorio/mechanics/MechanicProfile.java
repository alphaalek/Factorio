package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.util.Identifiable;

public interface MechanicProfile<M extends Mechanic<M>> extends Identifiable {

    String getName();

    default String getSignName() {
        return getName();
    }

    Building getBuilding(boolean hasWallSign);

    MechanicFactory<M> getFactory();

    StorageProvider getStorageProvider();

    MechanicLevel.Registry getLevelRegistry();

    default boolean isInteractable() {
        return false;
    }
}
