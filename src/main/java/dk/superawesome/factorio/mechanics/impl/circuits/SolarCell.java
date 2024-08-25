package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.AbstractRoute;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.transfer.EnergyCollection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.Set;

public class SolarCell extends AbstractMechanic<SolarCell> implements ThinkingMechanic, SignalSource, EnergyCollection {

    private final DelayHandler thinkHandler = new DelayHandler(20 * 60); // 1 minute

    private Block daylightSensor;
    private double energy;

    public SolarCell(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        daylightSensor = getLocation().getBlock();
        daylightSensor.setType(Material.DAYLIGHT_DETECTOR); //using day sensor, not night
    }

    @Override
    public MechanicProfile<SolarCell> getProfile() {
        return Profiles.SOLAR_CELL;
    }

    @Override
    public int getContext() {
        return SignalSource.TO_POWER_CENTRAL;
    }

    @Override
    public DelayHandler getThinkDelayHandler() {
        return thinkHandler;
    }

    @Override
    public void think() {
        if (daylightSensor.getLightFromSky() == 15 && !isAtMaxEnergy()) {
            energy += Math.random() * 55 + 20; // 20-75
            Routes.startSignalRoute(getLocation().getBlock(), this, false);
        } else if (hasEnergy())
            Routes.startSignalRoute(getLocation().getBlock(), this, false);
    }

    private boolean isAtMaxEnergy() {
        return energy >= 200;
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
