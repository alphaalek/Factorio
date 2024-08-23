package dk.superawesome.factorio.mechanics.transfer;

public interface EnergyCollection extends TransferCollection{

        int CAPACITY_MARK = 0;

        boolean hasEnergy();

        double take(double amount);
}
