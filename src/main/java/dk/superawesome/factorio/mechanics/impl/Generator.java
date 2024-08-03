package dk.superawesome.factorio.mechanics.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.impl.GeneratorGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.transfer.Fuel;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.block.data.type.Switch;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class Generator extends AbstractMechanic<Generator, GeneratorGui> implements FuelMechanic, ItemContainer, ThinkingMechanic, SignalSource, Lightable, SingleStorage {

    private final ThinkDelayHandler thinkDelayHandler = new ThinkDelayHandler(20);
    private Block lever;
    private Block campfire;

    private Fuel fuel;
    private int fuelAmount;
    private Fuel currentFuel;
    private float currentFuelAmount;

    private double provideEnergy;
    private boolean turnedOn;

    public Generator(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
        loadFromStorage();
    }

    @Override
    public void load(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayInputStream str = context.getData();

        loadFuel(context, str);
        this.provideEnergy = context.getSerializer().readDouble(str);
    }

    @Override
    public void save(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayOutputStream str = new ByteArrayOutputStream();

        saveFuel(context, str);
        context.getSerializer().writeDouble(str, this.provideEnergy);

        context.uploadData(str);
    }

    @Override
    public void blocksLoaded() {
        lever = getLocation().getBlock().getRelative(getRotation().getOppositeFace());
        campfire = getLocation().getBlock().getRelative(0, 2, 0);
        if (lever.getType() != Material.LEVER || campfire.getType() != Material.CAMPFIRE) {
            // invalid generator
            Factorio.get().getMechanicManager(getLocation().getWorld()).unload(this);
            return;
        }

        // update block state
        updateLight();
    }

    @Override
    public MechanicProfile<Generator, GeneratorGui> getProfile() {
        return Profiles.GENERATOR;
    }

    @Override
    public ThinkDelayHandler getDelayHandler() {
        return thinkDelayHandler;
    }

    @Override
    public void think() {
        // check if the generator does not have any energy ready for a power central
        if (provideEnergy == 0) {
            // ... use fuel and generate energy if not
            Fuel prevFuel = fuel;
            Fuel prevCurrentFuel = currentFuel;

            FuelState state = useFuel();
            if (state != FuelState.ABORT) {
                provideEnergy = prevCurrentFuel != null ? prevCurrentFuel.getEnergyAmount() : prevFuel.getEnergyAmount();

                GeneratorGui gui = inUse.get();
                if (gui != null) {
                    gui.updateFuelState();
                }
            }
        }

        // try to transfer energy if the generator has any energy ready
        if (provideEnergy > 0) {
            double prevProvideEnergy = provideEnergy;
            Routes.startSignalRoute(lever, this);

            if (provideEnergy == prevProvideEnergy && turnedOn) {
                // the generator was not able to transfer any energy, although it has energy to provide
                turnedOn = false;
                updateLight();
            }
        }
    }

    @Override
    public void updateLight() {
        if (loc.getWorld() != null) {
            BlockData data = this.campfire.getBlockData();
            if (data instanceof Campfire) {
                ((Campfire)data).setLit(this.turnedOn);
                this.campfire.setBlockData(data);
            }

            Switch lever = (Switch) this.lever.getBlockData();
            lever.setPowered(turnedOn);
            this.lever.setBlockData(lever);
        }
    }

    @Override
    public void pipePut(ItemCollection collection) {
        if (tickThrottle.isThrottled()) {
            return;
        }

        putFuel(collection, this, inUse, GeneratorGui::updateRemovedFuel);
    }

    public double takeEnergy(double energy) {
        double take = Math.min(provideEnergy, energy);
        provideEnergy -= provideEnergy;
        return take;
    }

    @Override
    public int getContext() {
        return SignalSource.TO_POWER_CENTRAL;
    }

    @Override
    public boolean handleOutput(Block block) {
        boolean transferred = Routes.transferEnergyToPowerCentral(block, this);
        if (transferred && !turnedOn) {
            // update light state if the generator was able to transfer energy, and it's currently not turned on
            // we don't have to check the other way around because that is just handled in the next self tick
            turnedOn = true;
            updateLight();
        }

        return transferred;
    }

    @Override
    public boolean isContainerEmpty() {
        return fuelAmount == 0;
    }

    @Override
    public int getCapacity() {
        return level.getInt(ItemCollection.CAPACITY_MARK);
    }

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
    public Fuel getFuel() {
        return this.fuel;
    }

    @Override
    public void setFuel(Fuel fuel) {
        this.fuel = fuel;
    }

    @Override
    public int getFuelAmount() {
        return this.fuelAmount;
    }

    @Override
    public void setFuelAmount(int amount) {
        this.fuelAmount = amount;

        if (this.fuelAmount == 0) {
            fuel = null;
        }
    }

    @Override
    public Fuel getCurrentFuel() {
        return this.currentFuel;
    }

    @Override
    public void setCurrentFuel(Fuel fuel) {
        this.currentFuel = fuel;
    }

    @Override
    public float getCurrentFuelAmount() {
        return this.currentFuelAmount;
    }

    @Override
    public void setCurrentFuelAmount(float amount) {
        this.currentFuelAmount = amount;
    }

    @Override
    public void removeFuel(int amount) {
        GeneratorGui gui = inUse.get();
        if (gui != null) {
            gui.updateRemovedFuel(amount);
        }
    }

    @Override
    public int getFuelCapacity() {
        return getCapacity();
    }
}
