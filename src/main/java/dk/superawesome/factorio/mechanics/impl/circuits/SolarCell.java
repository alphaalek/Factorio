package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.transfer.EnergyCollection;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.weather.WeatherEvent;

import java.util.Set;

public class SolarCell extends AbstractMechanic<SolarCell> implements SignalSource, EnergyCollection {

    private Block daylightSensor;
    private long lastEnergyCharge;
    private double energy;

    public SolarCell(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);

        lastEnergyCharge = System.currentTimeMillis();
    }

    @Override
    public void onBlocksLoaded(Player by) {
        daylightSensor = getLocation().getBlock().getRelative(BlockFace.UP);
        if (daylightSensor.getType() != org.bukkit.Material.DAYLIGHT_DETECTOR) {
            Factorio.get().getMechanicManager(getLocation().getWorld()).unload(this);
            Buildings.remove(loc.getWorld(), this);
        }
    }

    @Override
    public MechanicProfile<SolarCell> getProfile() {
        return null; // Profiles.SOLAR_CELL;
    }

    @Override
    public int getContext() {
        return SignalSource.TO_POWER_CENTRAL;
    }

    @EventHandler
    public void insteadOfRunnable(WeatherEvent event) {
        if (daylightSensor.getLightLevel() > 13 && canCharge() && !isAtMaxEnergy()) {
            double randomNum = Math.random() * 55 + 10; // 10-65
            lastEnergyCharge = System.currentTimeMillis();
            energy += (randomNum / 50) * daylightSensor.getLightLevel(); // 0.2-1.3 * lightLevel
            Routes.startSignalRoute(getLocation().getBlock(), this, false);
        } else if (canCharge()) {
            Routes.startSignalRoute(getLocation().getBlock(), this, false);
        }
    }

    private boolean canCharge() {
        long TIME_BETWEEN_CHARGES = 1000 * 60 * 5; // 5 minutes
        return System.currentTimeMillis() - lastEnergyCharge > TIME_BETWEEN_CHARGES;
    }

    private boolean isAtMaxEnergy() {
        return energy >= 100;
    }

    @Override
    public boolean handleOutput(Block block, Set<AbstractRoute.Signal> route) {
        return Routes.transferEnergyToPowerCentral(block, this);
    }

    @Override
    public boolean hasEnergy() {
        return energy > 0;
    }

    @Override
    public double take(double amount) {
        double take = Math.min(energy, amount);
        energy -= take;
        return take;
    }

    @Override
    public boolean isTransferEmpty() {
        return energy == 0;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return DelayHandler.NO_DELAY;
    }

    @Override
    public int getMaxTransfer() {
        return (int) energy;
    }

    @Override
    public int getTransferAmount() {
        return (int) energy;
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d / 30d;
    }

}
