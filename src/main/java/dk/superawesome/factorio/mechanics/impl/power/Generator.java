package dk.superawesome.factorio.mechanics.impl.power;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.SingleStorageGui;
import dk.superawesome.factorio.gui.impl.GeneratorGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.stackregistry.Fuel;
import dk.superawesome.factorio.mechanics.transfer.EnergyCollection;
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

    public Generator(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public void loadData(ByteArrayInputStream data) throws IOException, ClassNotFoundException {
        loadFuel(this.context, data);
        this.availableEnergy = this.context.getSerializer().readDouble(data);

        if (this.fuelAmount > 0 && this.fuel == null) {
            this.fuelAmount = 0;
        } else if (this.fuelAmount == 0 && this.fuel != null) {
            this.fuel = null;
        }
    }

    @Override
    public Optional<ByteArrayOutputStream> saveData() throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        saveFuel(this.context, data);
        this.context.getSerializer().writeDouble(data, this.availableEnergy);

        return Optional.of(data);
    }

    @Override
    public void onUpgrade(int newLevel) {
        this.thinkDelayHandler.setDelay(this.level.getInt(MechanicLevel.THINK_DELAY_MARK));
        super.onUpgrade(newLevel);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        this.lever = this.loc.getBlock().getRelative(this.rot.getOppositeFace());
        this.campfire = this.loc.getBlock().getRelative(0, 2, 0);
        if (this.lever.getType() != Material.LEVER || this.campfire.getType() != Material.CAMPFIRE) {
            // invalid generator
            Factorio.get().getMechanicManagerFor(this).deleteMechanic(this);
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
        return this.thinkDelayHandler;
    }

    @Override
    public void think() {
        // check if the generator does not have any energy available for a power central
        if (this.availableEnergy <= getTransferEnergyCost() * 2) {
            // ... use fuel and generate energy if not
            Fuel prevFuel = this.fuel;
            Fuel prevCurrentFuel = this.currentFuel;

            FuelState state = useFuel();
            if (state != FuelState.ABORT) {
                Fuel useFuel = prevCurrentFuel != null ? prevCurrentFuel : prevFuel; // both can't be null, but has to check current fuel first
                this.availableEnergy += useFuel.getEnergyAmount();

                this.xp += this.xpDist.poll();

                GeneratorGui gui = this.<GeneratorGui>getGuiInUse().get();
                if (gui != null) {
                    gui.updateFuelState();
                }
            }
        }

        // try to transfer energy if the generator has any energy available
        if (this.availableEnergy > 0) {
            double prevProvideEnergy = this.availableEnergy;
            Routes.startSignalRoute(this.lever, this, true, false);

            if (this.availableEnergy == prevProvideEnergy && this.turnedOn) {
                // the generator was not able to transfer any energy, although it has energy to provide, so turn off
                this.turnedOn = false;
                updateLight();
            }
        } else if (this.turnedOn) {
            // turn off after all energy has been transferred to a power central
            this.turnedOn = false;
            updateLight();
        }
    }

    @Override
    public void updateLight() {
        if (this.loc.getWorld() != null) {
            BlockData data = this.campfire.getBlockData();
            if (data instanceof Campfire) {
                ((Campfire)data).setLit(this.turnedOn);
                this.campfire.setBlockData(data);
            }

            Switch lever = (Switch) this.lever.getBlockData();
            lever.setPowered(this.turnedOn);
            this.lever.setBlockData(lever);
        }
    }

    @Override
    public void pipePut(ItemCollection collection, PipePutEvent event) {
        if (this.tickThrottle.isThrottled()) {
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
        if (transferred && !this.turnedOn) {
            // update light state if the generator was able to transfer energy, and it's currently not turned on
            // we don't have to check the other way around because that is just handled in the next self tick
            this.turnedOn = true;
            updateLight();
        }

        return transferred;
    }

    @Override
    public boolean isContainerEmpty() {
        return this.fuelAmount == 0;
    }

    @Override
    public int getCapacity() {
        return getCapacitySlots(this.level) *
                Optional.ofNullable(this.fuel)
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

        if (this.fuel == null) {
            this.fuelAmount = 0;
        }
    }

    @Override
    public int getFuelAmount() {
        return this.fuelAmount;
    }

    @Override
    public void setFuelAmount(int amount) {
        this.fuelAmount = amount;

        if (this.fuelAmount == 0) {
            this.fuel = null;
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
        double take = Math.min(this.availableEnergy, amount);
        this.availableEnergy -= take;
        return take;
    }

    @Override
    public boolean canBeDeleted() {
        return isContainerEmpty();
    }

    @Override
    public boolean isTransferEmpty() {
        return this.availableEnergy == 0;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return this.thinkDelayHandler;
    }

    @Override
    public double getMaxTransfer() {
        return this.availableEnergy;
    }

    @Override
    public double getTransferAmount() {
        return this.availableEnergy;
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d / 32d;
    }
}
