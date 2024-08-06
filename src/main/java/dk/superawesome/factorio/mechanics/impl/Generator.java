package dk.superawesome.factorio.mechanics.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.impl.GeneratorGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.routes.events.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.block.data.type.Switch;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Generator extends AbstractMechanic<Generator> implements FuelMechanic, ItemContainer, ThinkingMechanic, SignalSource, Lightable, Storage {

    private static final List<BlockVector> WASTE_OUTPUT_RELATIVES = Arrays.asList(
            new BlockVector(0, 1, 1),
            new BlockVector(0, 1, -1),
            new BlockVector(1, 1, 0),
            new BlockVector(-1, 1, 0)
    );

    private final ThinkDelayHandler thinkDelayHandler = new ThinkDelayHandler(20);
    private Block lever;
    private Block campfire;

    private Fuel fuel;
    private int fuelAmount;
    private Fuel currentFuel;
    private float currentFuelAmount;

    private double availableEnergy;
    private boolean turnedOn;

    public Generator(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
        loadFromStorage();
    }

    @Override
    public void load(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayInputStream str = context.getData();

        loadFuel(context, str);
        this.availableEnergy = context.getSerializer().readDouble(str);
    }

    @Override
    public void save(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayOutputStream str = new ByteArrayOutputStream();

        saveFuel(context, str);
        context.getSerializer().writeDouble(str, this.availableEnergy);

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
    public MechanicProfile<Generator> getProfile() {
        return Profiles.GENERATOR;
    }

    @Override
    public ThinkDelayHandler getDelayHandler() {
        return thinkDelayHandler;
    }

    @Override
    public void think() {
        // check if the generator does not have any energy available for a power central
        if (availableEnergy == 0) {
            // ... use fuel and generate energy if not
            Fuel prevFuel = fuel;
            Fuel prevCurrentFuel = currentFuel;

            FuelState state = useFuel();
            if (state != FuelState.ABORT) {
                availableEnergy = prevCurrentFuel != null ? prevCurrentFuel.getEnergyAmount() : prevFuel.getEnergyAmount(); // both can't be null, but has to check current fuel first

                GeneratorGui gui = this.<GeneratorGui>getGuiInUse().get();
                if (gui != null) {
                    gui.updateFuelState();
                }
            }
        }

        // try to transfer energy if the generator has any energy available
        if (availableEnergy > 0) {
            double prevProvideEnergy = availableEnergy;
            Routes.startSignalRoute(lever, this);

            if (availableEnergy == prevProvideEnergy && turnedOn) {
                // the generator was not able to transfer any energy, although it has energy to provide, so turn off
                turnedOn = false;
                updateLight();
            }
        } else if (turnedOn) {
            // turn off after all energy has been transferred to a power central
            turnedOn = false;
            updateLight();
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
    public void pipePut(ItemCollection collection, PipePutEvent event) {
        if (tickThrottle.isThrottled()) {
            return;
        }

        this.<GeneratorGui>putFuel(collection, this, event, getGuiInUse(), GeneratorGui::updateAddedItems);
    }

    public double takeEnergy(double energy) {
        double take = Math.min(availableEnergy, energy);
        availableEnergy -= take;
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
        GeneratorGui gui = this.<GeneratorGui>getGuiInUse().get();
        if (gui != null) {
            gui.updateRemovedItems(amount);
        }
    }

    @Override
    public int getFuelCapacity() {
        return getCapacity();
    }

    @Override
    public List<BlockVector> getWasteOutputs() {
        return WASTE_OUTPUT_RELATIVES;
    }
}
