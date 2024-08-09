package dk.superawesome.factorio.mechanics.transfer;

import dk.superawesome.factorio.mechanics.DelayHandler;

public interface TransferCollection {

    boolean isTransferEmpty();

    DelayHandler getTransferDelayHandler();

    int getMaxTransfer();

    int getTransferAmount();

    double getTransferEnergyCost();
}
