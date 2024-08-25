package dk.superawesome.factorio.mechanics.transfer;

import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;

import java.util.Set;

public interface Container<C extends TransferCollection> {

    boolean accepts(TransferCollection collection);

    boolean isContainerEmpty();

    void pipePut(C collection, Set<AbstractRoute.Pipe> route, PipePutEvent event);

    int getCapacity();

    interface HeapToStackAccess<T> {

        T get();

        void set(T val);
    }
}
