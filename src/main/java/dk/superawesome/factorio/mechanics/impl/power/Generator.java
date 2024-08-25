package dk.superawesome.factorio.mechanics.impl.power;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.impl.GeneratorGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.stackregistry.Fuel;
import dk.superawesome.factorio.mechanics.transfer.EnergyCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Generator extends AbstractMechanic<Generator> implements FuelMechanic, AccessibleMechanic, ThinkingMechanic, ItemContainer, SignalSource, Lightable, Storage, EnergyCollection {

    private static final List<BlockVector> WASTE_OUTPUT_RELATIVES = Arrays.asList(
            new BlockVector(0, 1, 1),
            new BlockVector(0, 1, -1),
            new BlockVector(1, 1, 0),
            new BlockVector(-1, 1, 0),

            new BlockVector(0, 0, 1),
            new BlockVector(0, 0, -1),
            new BlockVector(1, 0, 0),
            new BlockVector(-1, 0, 0)
    );

    private final DelayHandler thinkDelayHandler = new DelayHandler(20);
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
    public void load(MechanicStorageContext context) throws IOException, SQLException, ClassNotFoundException {
        ByteArrayInputStream str = context.getData();

        loadFuel(context, str);
        this.availableEnergy = context.getSerializer().readDouble(str);

        if (this.fuelAmount > 0 && this.fuel == null) {
            this.fuelAmount = 0;
        } else if (this.fuelAmount == 0 && this.fuel != null) {
            this.fuel = null;
        }
    }

    @Override
    public void save(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayOutputStream str = new ByteArrayOutputStream();

        saveFuel(context, str);
        context.getSerializer().writeDouble(str, this.availableEnergy);

        context.uploadData(str);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        lever = getLocation().getBlock().getRelative(rot.getOppositeFace());
        campfire = getLocation().getBlock().getRelative(0, 2, 0);
        if (lever.getType() != Material.LEVER || campfire.getType() != Material.CAMPFIRE) {
            // invalid generator
            Factorio.get().getMechanicManager(getLocation().getWorld()).unload(this);
            Buildings.remove(loc.getWorld(), this);
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
    public DelayHandler getThinkDelayHandler() {
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
            Routes.startSignalRoute(lever, this, true, false);

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

    private double takeEnergy(double energy) {
        double take = Math.min(availableEnergy, energy);
        availableEnergy -= take;
        return take;
    }

    @Override
    public int getContext() {
        return SignalSource.TO_POWER_CENTRAL;
    }

    @Override
    public boolean handleOutput(Block block, Location loc, Block from) {
        boolean transferred = Routes.transferEnergyToPowerCentral(block, loc, this);
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
        return level.getInt(ItemCollection.CAPACITY_MARK) *
                Optional.ofNullable(fuel)
                        .map(Fuel::getMaterial)
                        .map(Material::getMaxStackSize)
                        .orElse(64);
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

    @Override
    public boolean hasEnergy() {
        return availableEnergy > 0;
    }

    @Override
    public double take(double amount) {
        return takeEnergy(amount);
    }

    @Override
    public boolean canBeDeleted() {
        return isContainerEmpty();
    }

    @Override
    public boolean isTransferEmpty() {
        return availableEnergy == 0;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return thinkDelayHandler;
    }

    @Override
    public int getMaxTransfer() {
        return (int) availableEnergy;
    }

    @Override
    public int getTransferAmount() {
        return (int) availableEnergy;
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d / 20d;
    }
}
