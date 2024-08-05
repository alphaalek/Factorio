package dk.superawesome.factorio.mechanics;

public interface StorageProvider<M extends Mechanic<M>> {

    Storage createStorage(M mechanic, int context);
}
