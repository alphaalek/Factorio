package dk.superawesome.factorio.mechanics.impl.accessible;

import dk.superawesome.factorio.gui.impl.LiquidTankGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.stackregistry.Fluid;
import dk.superawesome.factorio.mechanics.transfer.FluidCollection;
import dk.superawesome.factorio.mechanics.transfer.FluidContainer;
import dk.superawesome.factorio.util.statics.StringUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import static dk.superawesome.factorio.util.statics.StringUtil.formatNumber;

public class LiquidTank extends AbstractMechanic<LiquidTank> implements FluidCollection, FluidContainer, AccessibleMechanic {

    private final DelayHandler transferDelayHandler = new DelayHandler(10);

    private Fluid fluid;
    private int fluidAmount;

    public LiquidTank(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public MechanicProfile<LiquidTank> getProfile() {
        return Profiles.LIQUID_TANK;
    }

    @Override
    public void loadData(ByteArrayInputStream data) throws IOException {
        int fluidOrdinal = data.read();
        if (fluidOrdinal <= Fluid.values().length && fluidOrdinal >= 0) {
            this.fluid = Fluid.values()[fluidOrdinal];
        }
        this.fluidAmount = this.context.getSerializer().readInt(data);

        if (this.fluidAmount > 0 && fluid == null) {
            this.fluidAmount = 0;
        } else if (this.fluidAmount == 0 && fluid != null) {
            this.fluid = null;
        }
    }

    @Override
    public Optional<ByteArrayOutputStream> saveData() throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(Optional.ofNullable(this.fluid).map(Enum::ordinal).orElse(-1));
        this.context.getSerializer().writeInt(data, this.fluidAmount);

        return Optional.of(data);
    }

    @Override
    public void onUpdate() {
        Sign sign = getSign();
        sign.getSide(Side.FRONT).setLine(2, formatNumber(((double) this.fluidAmount) / getCapacity() * 100) + "% fyldt");
        sign.update();
    }

    @Override
    public boolean canBeDeleted() {
        return true;
    }

    @Override
    public void pipePut(FluidCollection collection, PipePutEvent event) {
        if ((fluid == null || collection.hasFluid(fluid)) && this.fluidAmount < getCapacity()) {
            Fluid takeFluid = collection.getFluid();
            int take = collection.take((int) Math.min(getMaxTransfer(), getCapacity() - this.fluidAmount));

            if (take > 0) {
                this.fluid = takeFluid;
                this.fluidAmount += take;

                event.setTransferred(true);

                LiquidTankGui gui = this.<LiquidTankGui>getGuiInUse().get();
                if (gui != null) {
                    gui.updateItems();
                }
            }
        }
    }

    @Override
    public int take(int amount) {
        int take = Math.min(this.fluidAmount, amount);
        if (take > 0) {
            this.fluidAmount -= take;

            LiquidTankGui gui = this.<LiquidTankGui>getGuiInUse().get();
            if (gui != null) {
                gui.updateItems();
            }
        }

        return take;
    }

    @Override
    public boolean isContainerEmpty() {
        return this.fluid == null;
    }

    @Override
    public int getCapacity() {
        return this.level.getInt(FluidCollection.CAPACITY_MARK);
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
        return Optional.ofNullable(this.fluid).map(Fluid::getMaxTransfer).orElse(3);
    }

    @Override
    public double getTransferAmount() {
        return this.fluidAmount;
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
        return this.fluid;
    }

    public void setFluid(Fluid fluid) {
        this.fluid = fluid;

        if (this.fluid == null) {
            this.fluidAmount = 0;
        }
    }

    public int getFluidAmount() {
        return this.fluidAmount;
    }

    public void setFluidAmount(int amount) {
        this.fluidAmount = amount;

        if (this.fluidAmount == 0) {
            this.fluid = null;
        }
    }
}
