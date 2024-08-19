package dk.superawesome.factorio.mechanics.impl.behaviour;

import dk.superawesome.factorio.gui.impl.LiquidTankGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.stackregistry.Fluid;
import dk.superawesome.factorio.mechanics.transfer.FluidCollection;
import dk.superawesome.factorio.mechanics.transfer.FluidContainer;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class LiquidTank extends AbstractMechanic<LiquidTank> implements FluidCollection, FluidContainer {

    private final DelayHandler transferDelayHandler = new DelayHandler(10);

    private Fluid fluid;
    private int fluidAmount;

    public LiquidTank(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public MechanicProfile<LiquidTank> getProfile() {
        return Profiles.LIQUID_TANK;
    }

    @Override
    public void pipePut(FluidCollection collection, PipePutEvent event) {
        if ((fluid == null || collection.hasFluid(fluid)) && fluidAmount < getCapacity()) {
            int take = collection.take(getMaxTransfer());

            if (take > 0) {
                fluidAmount += take;
                event.setTransferred(true);

                LiquidTankGui gui = this.<LiquidTankGui>getGuiInUse().get();
                if (gui != null) {
                    gui.loadInputOutputItems();
                }
            }
        }
    }

    @Override
    public int take(int amount) {
        int take = Math.min(this.fluidAmount, amount);
        this.fluidAmount -= take;

        LiquidTankGui gui = this.<LiquidTankGui>getGuiInUse().get();
        if (gui != null) {
            gui.loadInputOutputItems();
        }

        return 0;
    }

    @Override
    public boolean isContainerEmpty() {
        return fluid == null;
    }

    @Override
    public int getCapacity() {
        return level.getInt(FluidCollection.CAPACITY_MARK);
    }

    @Override
    public boolean isTransferEmpty() {
        return fluid == null;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return transferDelayHandler;
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
        return 5d / 7d;
    }

    @Override
    public boolean hasFluid(Fluid fluid) {
        return false;
    }

    @Override
    public Fluid getFluid() {
        return fluid;
    }

    public void setFluid(Fluid fluid) {
        this.fluid = fluid;

        if (this.fluid == null) {
            this.fluidAmount = 0;
        }
    }

    public int getFluidAmount() {
        return fluidAmount;
    }

    public void setFluidAmount(int amount) {
        this.fluidAmount = amount;

        if (this.fluidAmount == 0) {
            this.fluid = null;
        }
    }
}
