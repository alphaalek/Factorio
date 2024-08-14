package dk.superawesome.factorio.mechanics.transfer;

public interface MoneyCollection extends TransferCollection {

    int CAPACITY_MARK = 1;

    double take(double amount);
}
