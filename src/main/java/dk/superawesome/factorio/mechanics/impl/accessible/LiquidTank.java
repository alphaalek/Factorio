package dk.superawesome.factorio.mechanics.impl.accessible;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.impl.LiquidTankGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.stackregistry.Fluid;
import dk.superawesome.factorio.mechanics.transfer.FluidCollection;
import dk.superawesome.factorio.mechanics.transfer.FluidContainer;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class LiquidTank extends AbstractMechanic<LiquidTank> implements FluidCollection, FluidContainer, AccessibleMechanic {

    private final DelayHandler transferDelayHandler = new DelayHandler(10);

    private Fluid fluid;
    private int fluidAmount;

    public LiquidTank(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
        loadFromStorage();
    }

    @Override
    public MechanicProfile<LiquidTank> getProfile() {
        return Profiles.LIQUID_TANK;
    }

    @Override
    public void load(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayInputStream str = context.getData();
        int fluidOrdinal = str.read();
        if (fluidOrdinal <= Fluid.values().length && fluidOrdinal >= 0) {
            this.fluid = Fluid.values()[fluidOrdinal];
        }
        this.fluidAmount = context.getSerializer().readInt(str);

        if (this.fluidAmount < 0) {
            Factorio.get().getLogger().warning("Storage amount for "+getProfile().getName()+" at " + getLocation() + " was negative, setting to 0");
            this.fluidAmount = 0;
        }
        if (this.fluidAmount > 0 && fluid == null) {
            this.fluidAmount = 0;
        } else if (this.fluidAmount == 0 && fluid != null) {
            this.fluid = null;
        }
    }

    @Override
    public void save(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        str.write(Optional.ofNullable(fluid).map(Enum::ordinal).orElse(-1));
        context.getSerializer().writeInt(str, fluidAmount);

        context.uploadData(str);
    }

    @Override
    public boolean canBeDeleted() {
        return true;
    }

    @Override
    public void pipePut(FluidCollection collection, PipePutEvent event) {
        if ((fluid == null || collection.hasFluid(fluid)) && fluidAmount < getCapacity()) {
            Fluid takeFluid = collection.getFluid();
            int take = collection.take(Math.min(getMaxTransfer(), getCapacity() - fluidAmount));

            if (take > 0) {
                fluidAmount += take;
                if (fluid == null) {
                    fluid = takeFluid;
                }
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
        setFluidAmount(this.fluidAmount - take); // can potentially be zero

        LiquidTankGui gui = this.<LiquidTankGui>getGuiInUse().get();
        if (gui != null) {
            gui.loadInputOutputItems();
        }

        return take;
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
        return Optional.ofNullable(fluid).map(Fluid::getMaxTransfer).orElse(3);
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
        return this.fluid == fluid;
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
