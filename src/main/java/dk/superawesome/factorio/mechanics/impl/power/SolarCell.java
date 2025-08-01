package dk.superawesome.factorio.mechanics.impl.power;

import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.transfer.EnergyCollection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class SolarCell extends AbstractMechanic<SolarCell> implements ThinkingMechanic, SignalSource, EnergyCollection {

    private static final int SUN_LIGHT = 15;
    private static final double MAX_ENERGY = 25d;

    private final DelayHandler thinkHandler = new DelayHandler(40);

    private double energy;

    public SolarCell(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        loc.getBlock().setType(Material.DAYLIGHT_DETECTOR); // using day sensor, not night
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
        return this.thinkHandler;
    }

    @Override
    public void think() {
        // night time
        if (this.loc.getWorld().getTime() > 12000) {
            return;
        }

        if (this.loc.getBlock().getLightFromSky() == SUN_LIGHT && energy < MAX_ENERGY) {
            energy += (Math.random() * 35 + 15) / 45; // 20 to 75 / 50 = 0.4 to 1.5
        }
        if (this.energy > 0) {
            Routes.startSignalRoute(this.loc.getBlock(), this, true, false);
        }
    }

    @Override
    public boolean handleOutput(Block block, Location loc, Block from) {
        return Routes.invokeEnergySourceOutput(block, loc, this, this);
    }

    @Override
    public double take(double amount) {
        double take = Math.min(this.energy, amount);
        this.energy -= take;
        return take;
    }

    @Override
    public boolean isTransferEmpty() {
        return this.energy == 0;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return DelayHandler.NO_DELAY;
    }

    @Override
    public double getMaxTransfer() {
        return MAX_ENERGY;
    }

    @Override
    public double getTransferAmount() {
        return this.energy;
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d / 8d;
    }
}
