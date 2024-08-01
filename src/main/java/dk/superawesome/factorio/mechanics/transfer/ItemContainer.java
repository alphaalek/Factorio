package dk.superawesome.factorio.mechanics.transfer;

public interface ItemContainer extends Container<ItemCollection> {

    default boolean accepts(TransferCollection collection) {
        return collection instanceof ItemCollection;
    }
}
