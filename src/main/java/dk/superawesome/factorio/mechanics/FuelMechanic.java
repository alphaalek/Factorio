package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.Fuel;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public interface FuelMechanic {

    int FUEL_CAPACITY = 2;

    Fuel getFuel();

    void setFuel(Fuel fuel);

    int getFuelAmount();

    void setFuelAmount(int amount);

    Fuel getCurrentFuel();

    void setCurrentFuel(Fuel fuel);

    float getCurrentFuelAmount();

    void setCurrentFuelAmount(float amount);

    void removeFuel(int amount);

    int getFuelCapacity();

    default <G extends BaseGui<G>> void putFuel(ItemCollection collection, Container<? extends TransferCollection> container, AtomicReference<G> inUse, BiConsumer<G, Integer> doGui) {
        if (getFuel() != null && collection.has(new ItemStack(getFuel().getMaterial())) || getFuel() == null && collection.has(i -> Fuel.getFuel(i.getType()) != null)) {
            int amount = container.put(collection, Math.min(64, getFuelCapacity() - getFuelAmount()), inUse, doGui, new Container.HeapToStackAccess<>() {
                @Override
                public ItemStack get() {
                    return getFuel() == null ? null : new ItemStack(getFuel().getMaterial());
                }

                @Override
                public void set(ItemStack stack) {
                    setFuel(Fuel.getFuel(stack.getType()));
                }
            });

            setFuelAmount(getFuelAmount() + amount);
        }
    }

    default FuelState useFuel() {
        Bukkit.broadcastMessage("Use fuel " + getCurrentFuel() + " " + getCurrentFuelAmount() + " " + getFuel() + " " + getFuelAmount());

        // if there are no fuel left, don't continue
        if (getCurrentFuelAmount() == 0 && getFuelAmount() == 0) {
            return FuelState.ABORT;
        }

        // use fuel
        if (getCurrentFuelAmount() == 0 && getFuelAmount() > 0) {
            // update view
            removeFuel(1);

            // remove the fuel
            setFuelAmount(getFuelAmount() - 1);
            setCurrentFuelAmount(1);
            setCurrentFuel(getFuel());
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
