package dk.superawesome.factorio.mechanics.transfer;

import dk.superawesome.factorio.mechanics.stackregistry.Fluid;

public interface FluidCollection extends TransferCollection {

    int CAPACITY_MARK = 0;

    boolean hasFluid(Fluid fluid);

    Fluid getFluid();

    int take(int amount);
}
