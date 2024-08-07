package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.mechanics.impl.Collector;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
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

    List<BlockVector> getWasteOutputs();

    Location getLocation();

    default <G extends BaseGui<G>> void putFuel(ItemCollection collection, Container<? extends TransferCollection> container, PipePutEvent event, AtomicReference<G> inUse, BiConsumer<G, Integer> doGui) {
        if (getFuel() != null && collection.has(new ItemStack(getFuel().getMaterial())) || getFuel() == null && collection.has(i -> Fuel.getFuel(i.getType()) != null)) {

            if (getFuelAmount() < getFuelCapacity()) {
                event.setTransferred(true);
                int amount = container.put(collection, getFuelCapacity() - getFuelAmount(), inUse, doGui, new Container.HeapToStackAccess<>() {
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
    }

    default FuelState useFuel() {
        // if there are no fuel left, don't continue
        if (getCurrentFuelAmount() == 0 && getFuelAmount() == 0) {
            return FuelState.ABORT;
        }

        if (getLocation().getWorld() != null) {
            getLocation().getWorld().playSound(getLocation(), Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE, 1.25f, 1f);
        }

        // use fuel
        if (getCurrentFuelAmount() == 0 && getFuelAmount() > 0) {
            // update view
            removeFuel(1);

            // remove the fuel
            setCurrentFuel(getFuel());
            setFuelAmount(getFuelAmount() - 1);
            setCurrentFuelAmount(1);
            if (getFuelAmount() == 0) {
                setFuel(null);
            }
        }
        if (getCurrentFuelAmount() > 0) {
            setCurrentFuelAmount(getCurrentFuelAmount() - getCurrentFuel().getFuelAmount());
            // due to working with floats, there can be calculation errors due to java binary encoding
            // this means that we can possibly end up with a number slightly above zero
            if (getCurrentFuelAmount() <= .001) {
                // done using current fuel, check for any waste
                if (getCurrentFuel().getWaste() != null) {
                    handleWaste(getLocation(), getCurrentFuel().getWaste());
                }
                setCurrentFuel(null);
                setCurrentFuelAmount(0); // ensure zero value (related problem mentioned above)

                return FuelState.SMELTED;
            }
        }

        return FuelState.SMELTING;
    }

    default void handleWaste(Location def, Material waste) {
        MechanicManager manager =  Factorio.get().getMechanicManager(def.getWorld());

        for (BlockVector vec : getWasteOutputs()) {
            // search for collectors to take the fuel waste
            Location loc = BlockUtil.getRel(def, vec);
            Block block = loc.getBlock();
            if (block.getType() == Material.HOPPER) {
                // check if the hopper is facing towards the mechanic
                if (manager.getMechanicPartially(BlockUtil.getPointingBlock(block, false).getLocation()) == this) {
                    Mechanic<?> mechanic = manager.getMechanicPartially(loc);
                    if (mechanic instanceof Collector collector) {
                        if (collector.handleInput(waste)) {
                            // the collector took the waste, just break
                            break;
                        }
                    }
                }
            }
        }
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

    default Storage convertFuelStorage() {
        return new Storage() {
            @Override
            public ItemStack getStored() {
                return Optional.ofNullable(getFuel()).map(Fuel::getMaterial).map(ItemStack::new).orElse(null);
            }

            @Override
            public void setStored(ItemStack stored) {
                setFuel(Fuel.getFuel(stored.getType()));
            }

            @Override
            public int getAmount() {
                return getFuelAmount();
            }

            @Override
            public void setAmount(int amount) {
                setFuelAmount(amount);
            }

            @Override
            public int getCapacity() {
                return getFuelCapacity();
            }
        };
    }

    enum FuelState {
        ABORT,

        SMELTING,

        SMELTED
    }
}
