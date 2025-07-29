package dk.superawesome.factorio.mechanics.impl.power;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.routes.impl.Signal;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class PowerCentral extends AbstractMechanic<PowerCentral> implements AccessibleMechanic, ThinkingMechanic, SignalSource, Lightable {

    public static final int CAPACITY = 0;

    private static final double SIGNAL_COST = 1d / 32d;

    private final DelayHandler thinkDelayHandler = new DelayHandler(level.get(MechanicLevel.THINK_DELAY_MARK));

    private boolean activated;
    private boolean hasGraph;
    private double recentProduction;
    private double recentConsumption;

    private Block lever;
    private double energy;
    private boolean turnedOn;
    private double recentMax;

    public PowerCentral(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public void loadData(ByteArrayInputStream data) throws IOException {
        this.energy = this.context.getSerializer().readDouble(data);
        this.activated = !this.context.getSerializer().readBoolean(data);
    }

    @Override
    public Optional<ByteArrayOutputStream> saveData() throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        this.context.getSerializer().writeDouble(data, this.energy);
        this.context.getSerializer().writeBoolean(data, !this.activated);

        return Optional.of(data);
    }

    @Override
    public void onUpgrade(int newLevel) {
        this.thinkDelayHandler.setDelay(this.level.getInt(MechanicLevel.THINK_DELAY_MARK));
        super.onUpgrade(newLevel);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        this.lever = getLocation().getBlock().getRelative(this.rot.getOppositeFace());
        if (this.lever.getType() != Material.LEVER) {
            // invalid power central
            Factorio.get().getMechanicManagerFor(this).deleteMechanic(this);
        } else {
            // update block state
            updateLight();
        }
    }

    @Override
    public MechanicProfile<PowerCentral> getProfile() {
        return Profiles.POWER_CENTRAL;
    }

    @Override
    public void think() {
        // check if the PC is activated or not
        if (!this.activated) {
            if (this.turnedOn) {
                this.turnedOn = false;
                updateLight();
            }

            // don't proceed to think
            return;
        }

        double prev = this.energy;
        if (this.energy > 0) {
            // send signal through the PC
            Routes.startSignalRoute(this.lever, this, true, false);

            if (!this.turnedOn && this.energy < prev) {
                // the PC used energy, power on
                this.turnedOn = true;
                updateLight();
            }
        }

        if (this.energy == prev && this.turnedOn) {
            // the PC didn't use any energy, power off
            this.turnedOn = false;
            updateLight();
        }
    }

    @Override
    public void updateLight() {
        Block block = this.loc.getBlock().getRelative(BlockFace.UP);
        Lightable.setLit(block, this.turnedOn);

        Switch lever = (Switch) this.lever.getBlockData();
        lever.setPowered(this.turnedOn);
        this.lever.setBlockData(lever);
    }

    @Override
    public DelayHandler getThinkDelayHandler() {
        return this.thinkDelayHandler;
    }

    public double getEnergy() {
        return this.energy;
    }

    public void setEnergy(double energy) {
        double cap = getCapacity();
        if (energy > cap) {
            this.energy = energy = cap;
        }

        if (this.hasGraph && energy > this.energy) {
            this.recentProduction += energy - this.energy;
        } else if (this.hasGraph && energy < this.energy) {
            this.recentConsumption += this.energy - energy;
        }

        this.energy = energy;
    }

    public double pollRecentProduction() {
        if (!this.hasGraph) {
            throw new UnsupportedOperationException();
        }

        double production = this.recentProduction;
        this.recentProduction = 0;
        return production;
    }

    public double pollRecentConsumption() {
        if (!this.hasGraph) {
            throw new UnsupportedOperationException();
        }

        double consumption = this.recentConsumption;
        this.recentConsumption = 0;
        return consumption;
    }

    public double getCapacity() {
        return this.level.get(CAPACITY);
    }

    public void setHasGraph(boolean hasGraph) {
        this.hasGraph = hasGraph;
        this.recentConsumption = 0;
        this.recentProduction = 0;
    }

    public boolean hasGraph() {
        return this.hasGraph;
    }

    @Override
    public boolean preSignal(Signal signal, boolean firstCall) {
        double signalCost = (signal.getLocations().size() - 1) * SIGNAL_COST;
        if (this.energy < signalCost) {
            if (this.recentMax == 0) {
                this.recentMax = signalCost;
            }

            return false;
        }

        if (firstCall) {
            this.recentMax = signalCost;
        }
        setEnergy(getEnergy() - signalCost);
        return true;
    }

    @Override
    public void postSignal(Signal signal, int outputs) {
        double signalCost = (signal.getLocations().size() - 1) * SIGNAL_COST;
        double ratio = (outputs == 0 || signal.getOutputs(FROM_POWER_CENTRAL).isEmpty()) ? 1 : ((double)outputs) / signal.getOutputs(FROM_POWER_CENTRAL).size();

        double back = signalCost * ratio;

        this.energy += back;
        this.recentConsumption -= back;
    }

    @Override
    public int getContext() {
        return SignalSource.FROM_POWER_CENTRAL;
    }

    @Override
    public boolean handleOutput(Block block, Location loc, Block from) {
        return Routes.invokePCOutput(block, loc, from, this);
    }

    public double getRecentMax() {
        return this.recentMax;
    }

    public void setRecentMax(double recentMax) {
        this.recentMax = recentMax;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public boolean isActivated() {
        return this.activated;
    }
}
