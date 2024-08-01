package dk.superawesome.factorio.mechanics.transfer;

public interface MoneyContainer extends Container<MoneyCollection> {

    default boolean accepts(TransferCollection collection) {
        return collection instanceof MoneyCollection;
    }
}
