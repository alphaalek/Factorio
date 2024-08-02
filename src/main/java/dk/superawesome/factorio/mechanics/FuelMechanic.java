package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.mechanics.transfer.Fuel;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface FuelMechanic {

    Fuel getFuel();

    void setFuel(Fuel fuel);

    int getFuelAmount();

    void setFuelAmount(int amount);

    Fuel getCurrentFuel();

    void setCurrentFuel(Fuel fuel);

    float getCurrentFuelAmount();

    void setCurrentFuelAmount(float amount);

    void removeFuel(int amount);

    default FuelState useFuel() {
        // if there are no fuel left, don't continue
        if (getCurrentFuelAmount() == 0 && getFuelAmount() == 0) {
            return FuelState.ABORT;
        }

        // use fuel
        if (getCurrentFuelAmount() == 0 && getFuelAmount() > 0) {
            removeFuel(1);

            // remove the fuel
            setFuelAmount(getFuelAmount() - 1);
            setCurrentFuelAmount(1);
            setCurrentFuel(getCurrentFuel());
            if (getFuelAmount() == 0) {
                setFuel(null);
            }
        }
        if (getCurrentFuelAmount() > 0) {
            setCurrentFuelAmount(getCurrentFuelAmount() - getCurrentFuel().getFuelAmount());
            // due to working with floats, there can be calculation errors due to java binary encoding
            // this means that we can possibly end up with a number slightly above zero
            if (getCurrentFuelAmount() <= .001) {
                setCurrentFuel(null);
                setCurrentFuelAmount(0); // ensure zero value (related problem mentioned above)

                return FuelState.SMELTED;
            }
        }

        return FuelState.SMELTING;
    }

    default void loadFuel(MechanicStorageContext context, ByteArrayInputStream str) throws IOException {
        ItemStack fuel = context.getSerializer().readItemStack(str);
        if (fuel != null) {
            setFuel(Fuel.getFuel(fuel.getType()));
        }
        setFuelAmount(context.getSerializer().readInt(str));
        ItemStack currentFuel = context.getSerializer().readItemStack(str);
        int currentFuelAmount = context.getSerializer().readInt(str);
        if (currentFuel != null) {
            setCurrentFuel(Fuel.getFuel(currentFuel.getType()));
            setCurrentFuelAmount(1 - getCurrentFuel().getFuelAmount() * currentFuelAmount);
        }
    }

    default void saveFuel(MechanicStorageContext context, ByteArrayOutputStream str) throws IOException {
        if (getFuel() != null) {
            context.getSerializer().writeItemStack(str, new ItemStack(getFuel().getMaterial()));
        } else {
            context.getSerializer().writeItemStack(str, null);
        }
        context.getSerializer().writeInt(str, getFuelAmount());
        if (getCurrentFuel() != null) {
            context.getSerializer().writeItemStack(str, new ItemStack(getCurrentFuel().getMaterial()));
            context.getSerializer().writeInt(str, (int) ((1 - getCurrentFuelAmount()) / getCurrentFuel().getFuelAmount()));
        } else {
            context.getSerializer().writeItemStack(str, null);
            context.getSerializer().writeInt(str, 0);
        }
    }

    enum FuelState {
        ABORT,

        SMELTING,

        SMELTED
    }
}
