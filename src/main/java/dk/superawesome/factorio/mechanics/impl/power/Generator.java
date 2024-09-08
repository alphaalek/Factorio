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

    private final XPDist xpDist = new XPDist(100, 0.0045, 0.0115);
    private final DelayHandler thinkDelayHandler = new DelayHandler(level.get(MechanicLevel.THINK_DELAY_MARK));
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
    public void onUpgrade(int newLevel) {
        thinkDelayHandler.setDelay(level.getInt(MechanicLevel.THINK_DELAY_MARK));
        super.onUpgrade(newLevel);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        lever = getLocation().getBlock().getRelative(this.rot.getOppositeFace());
        campfire = getLocation().getBlock().getRelative(0, 2, 0);
        if (lever.getType() != Material.LEVER || campfire.getType() != Material.CAMPFIRE) {
            // invalid generator
            Factorio.get().getMechanicManager(getLocation().getWorld()).unload(this);
            Buildings.remove(this, this.loc, this.rot, true);
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
        if (availableEnergy <= getTransferEnergyCost() * 2) {
            // ... use fuel and generate energy if not
            Fuel prevFuel = fuel;
            Fuel prevCurrentFuel = currentFuel;

            FuelState state = useFuel();
            if (state != FuelState.ABORT) {
                Fuel useFuel = prevCurrentFuel != null ? prevCurrentFuel : prevFuel; // both can't be null, but has to check current fuel first
                availableEnergy += useFuel.getEnergyAmount();

                xp += xpDist.poll();

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

    @Override
    public int getContext() {
        return SignalSource.TO_POWER_CENTRAL;
    }

    @Override
    public boolean handleOutput(Block block, Location loc, Block from) {
        boolean transferred = Routes.invokeEnergySourceOutput(block, loc, this, this);
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
        return getCapacitySlots(level) *
                Optional.ofNullable(fuel)
                        .map(Fuel::material)
                        .map(Material::getMaxStackSize)
                        .orElse(64);
    }

    @Override
    public ItemStack getStored() {
        return Optional.ofNullable(getFuel()).map(Fuel::material).map(ItemStack::new).orElse(null);
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
    public double take(double amount) {
        double take = Math.min(availableEnergy, amount);
        availableEnergy -= take;
        return take;
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
    public double getMaxTransfer() {
        return availableEnergy;
    }

    @Override
    public double getTransferAmount() {
        return availableEnergy;
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d / 32d;
    }
}
