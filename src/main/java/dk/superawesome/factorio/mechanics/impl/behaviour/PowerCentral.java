package dk.superawesome.factorio.mechanics.impl.behaviour;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;

public class PowerCentral extends AbstractMechanic<PowerCentral> implements AccessibleMechanic, ThinkingMechanic, SignalSource, Lightable {

    public static final int CAPACITY = 0;

    private static final double SIGNAL_COST = 1d / 32d;

    private final DelayHandler thinkDelayHandler = new DelayHandler(20);

    private boolean hasGraph;
    private double recentProduction;
    private double recentConsumption;

    private Block lever;
    private double energy;
    private boolean turnedOn;
    private double recentMax = 0;

    public PowerCentral(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
        loadFromStorage();
    }

    @Override
    public void load(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayInputStream str = context.getData();
        this.energy = context.getSerializer().readDouble(str);
    }

    @Override
    public void save(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        context.getSerializer().writeDouble(str, this.energy);

        context.uploadData(str);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        lever = getLocation().getBlock().getRelative(rot.getOppositeFace());
        if (lever.getType() != Material.LEVER) {
            // invalid power central
            Factorio.get().getMechanicManager(getLocation().getWorld()).unload(this);
            Buildings.remove(loc.getWorld(), this);
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
        double prev = energy;
        if (energy > 0) {
            Routes.startSignalRoute(lever, this, false);

            if (!turnedOn && energy < prev) {
                turnedOn = true;
                updateLight();
            }
        }

        if ((energy == 0 || energy == prev) && turnedOn) {
            turnedOn = false;
            updateLight();
        }
    }

    @Override
    public void updateLight() {
        if (loc.getWorld() != null) {
            Block block = loc.getWorld().getBlockAt(BlockUtil.getRel(loc, getProfile().getBuilding().getRelatives().get(1)));
            BlockData data = block.getBlockData();
            if (data instanceof org.bukkit.block.data.Lightable) {
                ((org.bukkit.block.data.Lightable)data).setLit(turnedOn);
                block.setBlockData(data);
            }

            Switch lever = (Switch) this.lever.getBlockData();
            lever.setPowered(turnedOn);
            this.lever.setBlockData(lever);
        }
    }

    @Override
    public DelayHandler getThinkDelayHandler() {
        return thinkDelayHandler;
    }

    public double getEnergy() {
        return energy;
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
        if (!hasGraph) {
            throw new UnsupportedOperationException();
        }

        double production = this.recentProduction;
        this.recentProduction = 0;
        return production;
    }

    public double pollRecentConsumption() {
        if (!hasGraph) {
            throw new UnsupportedOperationException();
        }

        double consumption = this.recentConsumption;
        this.recentConsumption = 0;
        return consumption;
    }

    public double getCapacity() {
        return level.get(CAPACITY);
    }

    public void setHasGraph(boolean hasGraph) {
        this.hasGraph = hasGraph;
        this.recentConsumption = 0;
        this.recentProduction = 0;
    }

    public boolean hasGraph() {
        return hasGraph;
    }

    @Override
    public boolean preSignal(AbstractRoute.Signal signal) {
        double signalCost = signal.getLocations().size() * SIGNAL_COST;
        if (this.energy < signalCost) {
            if (recentMax == 0) {
                recentMax = signalCost;
            }

            return false;
        }

        recentMax = signalCost;
        setEnergy(getEnergy() - signalCost);
        return true;
    }

    @Override
    public void postSignal(AbstractRoute.Signal signal, int outputs) {
        double signalCost = signal.getLocations().size() * SIGNAL_COST;
        double ratio = (outputs == 0 || signal.getOutputs(FROM_POWER_CENTRAL).isEmpty()) ? 1 : ((double)outputs) / signal.getOutputs(FROM_POWER_CENTRAL).size();

        double back = signalCost * ratio;

        energy += back;
        recentConsumption -= back;
    }

    @Override
    public int getContext() {
        return SignalSource.FROM_POWER_CENTRAL;
    }

    @Override
    public boolean handleOutput(Block block) {
        return Routes.invokeSignalOutput(block, this);
    }

    public double getRecentMax() {
        return recentMax;
    }

    public void setRecentMax(double recentMax) {
        this.recentMax = recentMax;
    }
}
