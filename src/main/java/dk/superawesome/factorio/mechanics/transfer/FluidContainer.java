package dk.superawesome.factorio.mechanics.transfer;

import org.bukkit.Material;

public interface FluidContainer extends Container<TransferCollection> {

    default boolean accepts(TransferCollection collection) {
        return collection instanceof FluidCollection;
    }


    int getCapacity();

    int getAmount();

    void setAmount(int amount);

    void addAmount(int amount);

    void removeAmount(int amount);

    void setFluidType(Material fluidType);

    Material getFluidType();
}
