package dk.superawesome.factorio.mechanics.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.impl.PowerCentralGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Switch;

import java.io.*;
import java.sql.SQLException;

public class PowerCentral extends AbstractMechanic<PowerCentral, PowerCentralGui> implements ThinkingMechanic {

    public static final int CAPACITY = 0;

    private final ThinkDelayHandler thinkDelayHandler = new ThinkDelayHandler(20);

    private boolean hasGraph;
    private double recentProduction;
    private double recentConsumption;

    private Block lever;
    private double energy;
    private boolean turnedOn;

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
    public void blocksLoaded() {
        lever = getLocation().getBlock().getRelative(getRotation().getOppositeFace());
        if (lever.getType() != Material.LEVER) {
            // invalid power central
            Factorio.get().getMechanicManager(getLocation().getWorld()).unload(this);
        }
    }

    @Override
    public MechanicProfile<PowerCentral, PowerCentralGui> getProfile() {
        return Profiles.POWER_CENTRAL;
    }

    @Override
    public void think() {
        double prev = energy;
        if (energy > 0) {
            Routes.startSignalRoute(lever, this);

            if (!turnedOn && energy < prev) {
                turnedOn = true;
                update();
            }
        }

        if ((energy == 0 || energy == prev) && turnedOn) {
            turnedOn = false;
            update();
        }
    }

    private void update() {
        if (loc.getWorld() != null) {
            Block block = loc.getWorld().getBlockAt(BlockUtil.getRel(loc, getProfile().getBuilding().getRelatives().get(1)));
            BlockData data = block.getBlockData();
            if (data instanceof Lightable) {
                ((Lightable)data).setLit(turnedOn);
                block.setBlockData(data);
            }

            Switch lever = (Switch) this.lever.getBlockData();
            lever.setPowered(turnedOn);
            this.lever.setBlockData(lever);
        }
    }

    @Override
    public ThinkDelayHandler getDelayHandler() {
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
        this.recentConsumption = 0;
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
}
