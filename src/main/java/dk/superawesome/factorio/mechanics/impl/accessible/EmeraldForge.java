package dk.superawesome.factorio.mechanics.impl.accessible;

import dk.superawesome.factorio.gui.impl.EmeraldForgeGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.MoneyCollection;
import dk.superawesome.factorio.mechanics.transfer.MoneyContainer;
import dk.superawesome.factorio.util.statics.StringUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static dk.superawesome.factorio.util.statics.StringUtil.formatNumber;

public class EmeraldForge extends AbstractMechanic<EmeraldForge> implements AccessibleMechanic, MoneyContainer {

    private double moneyAmount;

    public EmeraldForge(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public void loadData(ByteArrayInputStream data) throws IOException {
        this.moneyAmount = this.context.getSerializer().readDouble(data);
    }

    @Override
    public Optional<ByteArrayOutputStream> saveData() throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        this.context.getSerializer().writeDouble(data, this.moneyAmount);

        return Optional.of(data);
    }

    @Override
    public void onUpdate() {
        Sign sign = getSign();
        sign.getSide(Side.FRONT).setLine(2, formatNumber(this.moneyAmount / getCapacity() * 100) + "% fyldt");
        sign.update();
    }

    @Override
    public MechanicProfile<EmeraldForge> getProfile() {
        return Profiles.EMERALD_FORGE;
    }

    @Override
    public boolean isContainerEmpty() {
        return this.moneyAmount < .01; // can't be formatted with 2 decimals
    }

    @Override
    public void pipePut(MoneyCollection collection, PipePutEvent event) {
        double take = Math.min(64, getCapacity() - this.moneyAmount);
        double amount = collection.take(take);
        if (amount > 0) {
            this.moneyAmount += amount;
            event.setTransferred(true);

            EmeraldForgeGui gui = this.<EmeraldForgeGui>getGuiInUse().get();
            if (gui != null) {
                gui.updateItems();
            }
        }
    }

    @Override
    public int getCapacity() {
        return this.level.getInt(MoneyCollection.CAPACITY_MARK);
    }

    public double getMoneyAmount() {
        return this.moneyAmount;
    }

    public void setMoneyAmount(double amount) {
        this.moneyAmount = amount;
    }
}
