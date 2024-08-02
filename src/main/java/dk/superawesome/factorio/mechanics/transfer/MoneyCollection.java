package dk.superawesome.factorio.mechanics.transfer;

public interface MoneyCollection extends TransferCollection {

    int CAPACITY_MARK = 0;

    double take(double amount);
}
