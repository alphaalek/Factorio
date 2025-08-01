package dk.superawesome.factorio.mechanics.impl.other;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.stackregistry.Fluid;
import dk.superawesome.factorio.mechanics.transfer.FluidCollection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.CauldronLevelChangeEvent;

public class Cauldron extends AbstractMechanic<Cauldron> implements FluidCollection {

    private final DelayHandler transferDelayHandler = new DelayHandler(20 * 2);

    private Fluid fluid;
    private int amount;

    public Cauldron(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public MechanicProfile<Cauldron> getProfile() {
        return Profiles.CAULDRON;
    }

    private void checkCauldron() {
        this.fluid = null;
        this.amount = 0;
        if (loc.getBlock().getType() == Material.WATER_CAULDRON) {
            Levelled cauldron = (Levelled) this.loc.getBlock().getBlockData();
            this.amount = cauldron.getLevel();
            this.fluid = Fluid.WATER;
        } else {
            this.amount = 3;
            switch (this.loc.getBlock().getType()) {
                case LAVA_CAULDRON -> this.fluid = Fluid.LAVA;
                case POWDER_SNOW_CAULDRON -> this.fluid = Fluid.SNOW;
            }
        }
    }

    @EventHandler
    public void onCauldronLevelChange(CauldronLevelChangeEvent event) {
        if (event.getBlock().equals(this.loc.getBlock())) {
            Bukkit.getScheduler().runTask(Factorio.get(), this::checkCauldron);
        }
    }

    @Override
    public void onBlocksLoaded(Player by) {
        checkCauldron();
    }

    @Override
    public boolean isTransferEmpty() {
        return this.fluid == null;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return this.transferDelayHandler;
    }

    @Override
    public double getMaxTransfer() {
        return this.fluid.getMaxTransfer();
    }

    @Override
    public double getTransferAmount() {
        return this.amount;
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d / 4d;
    }

    @Override
    public Fluid getFluid() {
        return this.fluid;
    }

    @Override
    public boolean hasFluid(Fluid fluid) {
        return this.fluid != null && this.fluid.equals(fluid);
    }

    @Override
    public int take(int amount) {
        int take = Math.min(this.amount, amount);

        // if its trying to transfer less than the minimum, then return zero
        if (take < this.fluid.getMinTransfer()) {
            return 0;
        }

        this.amount -= take;
        if (this.amount == 0) {
            this.fluid = null;
            this.loc.getBlock().setType(Material.CAULDRON);
        } else {
            Levelled cauldron = (Levelled) this.loc.getBlock().getBlockData();
            cauldron.setLevel(this.amount);
            this.loc.getBlock().setBlockData(cauldron);
        }

        return take;
    }
}
