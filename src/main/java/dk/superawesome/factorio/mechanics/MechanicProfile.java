package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.util.Identifiable;

public interface MechanicProfile<M extends Mechanic<M>> extends Identifiable {

    String getName();

    default String getSignName() {
        return getName();
    }

    Building getBuilding(Mechanic<?> forMechanic);

    MechanicFactory<M> getFactory();

    StorageProvider<M> getStorageProvider();

    MechanicLevel.Registry getLevelRegistry();

    default boolean isInteractable() {
        return false;
    }
}
