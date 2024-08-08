package dk.superawesome.factorio.mechanics.impl.behaviour;

import dk.superawesome.factorio.gui.impl.EmeraldForgeGui;
import dk.superawesome.factorio.mechanics.AbstractMechanic;
import dk.superawesome.factorio.mechanics.MechanicProfile;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.mechanics.Profiles;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.MoneyCollection;
import dk.superawesome.factorio.mechanics.transfer.MoneyContainer;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;

public class EmeraldForge extends AbstractMechanic<EmeraldForge> implements MoneyContainer {

    private double moneyAmount;

    public EmeraldForge(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
        loadFromStorage();
    }

    @Override
    public void load(MechanicStorageContext context) throws SQLException, IOException {
        ByteArrayInputStream data = context.getData();
        this.moneyAmount = context.getSerializer().readDouble(data);
    }

    @Override
    public void save(MechanicStorageContext context) throws SQLException, IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        context.getSerializer().writeDouble(stream, this.moneyAmount);

        context.uploadData(stream);
    }

    @Override
    public MechanicProfile<EmeraldForge> getProfile() {
        return Profiles.EMERALD_FORGE;
    }

    @Override
    public boolean isContainerEmpty() {
        return moneyAmount < .01; // can't be formatted with 2 decimals
    }

    @Override
    public void pipePut(MoneyCollection collection, PipePutEvent event) {
        double take = Math.min(64, getCapacity() - moneyAmount);
        double amount = collection.take(take);
        moneyAmount += amount;
        event.setTransferred(true);

        EmeraldForgeGui gui = this.<EmeraldForgeGui>getGuiInUse().get();
        if (gui != null) {
            gui.updateAddedMoney(amount);
        }
    }

    @Override
    public int getCapacity() {
        return level.getInt(MoneyCollection.CAPACITY_MARK);
    }

    public double getMoneyAmount() {
        return this.moneyAmount;
    }

    public void setMoneyAmount(double amount) {
        this.moneyAmount = amount;
    }
}
