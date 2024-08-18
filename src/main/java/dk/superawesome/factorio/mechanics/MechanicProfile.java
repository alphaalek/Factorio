package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.util.Identifiable;

public interface MechanicProfile<M extends Mechanic<M>> extends Identifiable {

    String getName();

    Building getBuilding();

    MechanicFactory<M> getFactory();

    StorageProvider<M> getStorageProvider();

    MechanicLevel.Registry getLevelRegistry();

    default boolean isInteractable() {
        return false;
    }
}
