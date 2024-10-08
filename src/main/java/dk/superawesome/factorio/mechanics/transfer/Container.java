package dk.superawesome.factorio.mechanics.transfer;

import dk.superawesome.factorio.mechanics.MechanicLevel;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;

public interface Container<C extends TransferCollection> {

    boolean accepts(TransferCollection collection);

    boolean isContainerEmpty();

    void pipePut(C collection, PipePutEvent event);

    int getCapacity();

    default int getCapacitySlots(MechanicLevel level) {
        return level.getInt(ItemCollection.CAPACITY_MARK);
    }

    interface HeapToStackAccess<T> {

        T get();

        void set(T val);
    }
}
