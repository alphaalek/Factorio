package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.stackregistry.Fluid;
import dk.superawesome.factorio.mechanics.transfer.FluidCollection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.CauldronLevelChangeEvent;

import static org.bukkit.event.block.CauldronLevelChangeEvent.ChangeReason.UNKNOWN;

public class Cauldron extends AbstractMechanic<Cauldron> implements FluidCollection {

    private int fluidAmount;
    private Fluid fluid;

    public Cauldron(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public MechanicProfile<Cauldron> getProfile() {
        return Profiles.CAULDRON;
    }

    @EventHandler
    public void onCauldronLevelChange(CauldronLevelChangeEvent event) {
        if (event.getBlock().equals(loc.getBlock())) {
            this.fluidAmount = ((Levelled)event.getBlock().getBlockData()).getLevel() + 1;

            if (this.fluid == null) {
                switch (event.getBlock().getType()) {
                    case WATER_CAULDRON -> this.fluid = Fluid.WATER;
                    case LAVA_CAULDRON -> this.fluid = Fluid.LAVA;
                }
            }
        }
    }

    @Override
    public void onBlocksLoaded() {
        Bukkit.getPluginManager().callEvent(
            new CauldronLevelChangeEvent(loc.getBlock(), null, UNKNOWN, loc.getBlock().getState())
        );
    }

    @Override
    public boolean isTransferEmpty() {
        return fluidAmount == 0;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return DelayHandler.NO_DELAY;
    }

    @Override
    public int getMaxTransfer() {
        return fluid.getMaxTransfer();
    }

    @Override
    public int getTransferAmount() {
        return fluidAmount;
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d / 4d;
    }

    @Override
    public boolean hasFluid(Fluid fluid) {
        return this.fluid == fluid;
    }

    @Override
    public int take(int amount) {
        int take = Math.min(this.fluidAmount, amount);
        this.fluidAmount -= take;

        if (this.fluidAmount == 0) {
            this.fluid = null;
        }

        return take;
    }
}
