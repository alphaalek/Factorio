package dk.superawesome.factorio.mechanics.transfer;

public interface FluidContainer extends Container<FluidCollection> {

    default boolean accepts(TransferCollection collection) {
        return collection instanceof FluidCollection;
    }
}
