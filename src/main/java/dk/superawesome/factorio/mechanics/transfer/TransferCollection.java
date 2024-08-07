package dk.superawesome.factorio.mechanics.transfer;

public interface TransferCollection {

    boolean isTransferEmpty();

    int getMaxTransfer();

    int getTransferAmount();

    double getTransferEnergyCost();
}
