package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.stackregistry.Fluid;
import dk.superawesome.factorio.mechanics.stackregistry.FluidStack;
import dk.superawesome.factorio.mechanics.transfer.FluidCollection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.CauldronLevelChangeEvent;

import static org.bukkit.event.block.CauldronLevelChangeEvent.ChangeReason.UNKNOWN;

public class Cauldron extends AbstractMechanic<Cauldron> implements FluidCollection {

    private final FluidStack fluidStack;

    public Cauldron(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
        fluidStack = new FluidStack(null, 0);
    }

    @Override
    public MechanicProfile<Cauldron> getProfile() {
        return Profiles.CAULDRON;
    }

    @EventHandler
    public void onCauldronLevelChange(CauldronLevelChangeEvent event) {
        if (event.getBlock().equals(loc.getBlock())) {
            fluidStack.setAmount(event.getNewLevel());
            if (fluidStack.getFluid() == null) {
                switch (event.getNewState().getType()) {
                    case WATER_CAULDRON -> fluidStack.setFluid(Fluid.WATER);
                    case LAVA_CAULDRON -> fluidStack.setFluid(Fluid.LAVA);
                    default -> fluidStack.setFluid(null);
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
        return !fluidStack.isFull();
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return DelayHandler.NO_DELAY;
    }

    @Override
    public int getMaxTransfer() {
        return fluidStack.getFluid().getMaxTransfer();
    }

    @Override
    public int getTransferAmount() {
        return fluidStack.getAmount();
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d / 4d;
    }

    @Override
    public Fluid getFluid() {
        return fluidStack.getFluid();
    }

    @Override
    public boolean hasFluid(Fluid fluid) {
        return this.fluidStack.getFluid() == fluid;
    }

    @Override
    public FluidStack take(int amount) {
        int take = Math.min(getTransferAmount(), amount);
        fluidStack.setAmount(fluidStack.getAmount() - take);

        FluidStack tookFluidStack = new FluidStack(fluidStack.getFluid(), take);

        if (fluidStack.getAmount() == 0) {
            setCauldronToDefault();
        }

        return tookFluidStack;
    }

    private void setCauldronToDefault() {
        loc.getBlock().setType(Material.CAULDRON);
        fluidStack.setFluid(null);
    }
}
