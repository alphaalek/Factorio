package dk.superawesome.factories.mechanics.impl;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.gui.impl.PowerCentralGui;
import dk.superawesome.factories.mechanics.*;
import dk.superawesome.factories.mechanics.profiles.PowerCentralProfile;
import dk.superawesome.factories.mechanics.routes.Routes;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Switch;

import java.io.*;

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
    public void load(MechanicStorageContext context) throws IOException {
        ByteArrayInputStream str = context.getData();
        this.energy = new DataInputStream(str).readDouble();
    }

    @Override
    public void save(MechanicStorageContext context) throws IOException {
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        new DataOutputStream(str).writeDouble(this.energy);

        context.upload(str);
    }

    @Override
    public void blocksLoaded() {
        lever = getLocation().getBlock().getRelative(getRotation().getOppositeFace());
        if (lever.getType() != Material.LEVER) {
            // invalid power central
            Factories.get().getMechanicManager(getLocation().getWorld()).unload(this);
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
            energy = cap - this.energy;
            this.energy = cap;
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
    }

    public boolean hasGraph() {
        return hasGraph;
    }
}
