package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.TickThrottle;
import dk.superawesome.factorio.util.db.Types;
import dk.superawesome.factorio.util.statics.StringUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public abstract class AbstractMechanic<M extends Mechanic<M>> implements Mechanic<M> {

    protected final AtomicReference<? extends BaseGui<?>> inUse = new AtomicReference<>();
    protected final TickThrottle tickThrottle = new TickThrottle();
    protected final Location loc;
    protected final BlockFace rot;
    protected final MechanicStorageContext context;
    protected final Management management;

    protected MechanicLevel level;
    protected double xp;
    private boolean exists = true;

    public AbstractMechanic(Location loc, BlockFace rotation, MechanicStorageContext context) {
        this.loc = loc;
        this.rot = rotation;
        this.context = context;

        try {
            this.level = MechanicLevel.from(this, context.getLevel());
            this.xp = context.getXP();
            this.management = context.getManagement();
        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Failed to load mechanic " + this  + " at " + Types.LOCATION.convert(loc), ex);
        }
    }

    protected void loadFromStorage() {
        try  {
            if (this.context.getController().validConnection() && this.context.hasContext()) {
                load(this.context);
            }
        } catch (Exception ex) {
            Factorio.get().getLogger().log(Level.SEVERE, "Failed to load data for mechanic " + this  + ", " + getLocation(), ex);
        }
    }

    @Override
    public void unload() {
        save();
        exists = false;
    }

    @Override
    public void save() {
        try {
            if (!this.context.getController().validConnection()) {
                return;
            }

            // ensure record exists
            if (!this.context.hasContext()) {
                Factorio.get().getContextProvider().create(this.loc, this.rot, getProfile().getName(), this.management.getOwner());
            }

            // save data for this mechanic
            this.context.setLevel(this.level.lvl());
            this.context.setXP(this.xp);
            this.context.uploadManagement(this.management);

            save(this.context);
        } catch (Exception ex) {
            Factorio.get().getLogger().log(Level.SEVERE, "Failed to save mechanic " + this + ", " + getLocation(), ex);
        }
    }

    @Override
    public boolean exists() {
        return exists;
    }

    public void load(MechanicStorageContext context) throws Exception {
        // to be overridden if needed
    }

    public void save(MechanicStorageContext context) throws Exception {
        // to be overridden if needed
    }

    protected Sign getSign() {
        return (Sign) loc.getBlock().getRelative(rot).getState();
    }

    @Override
    public void onUpgrade(int newLevel) {
        Sign sign = getSign();
        sign.getSide(Side.FRONT).setLine(1, "Lvl " + newLevel);
        sign.update();
    }

    @Override
    public void onBlocksLoaded(Player by) {

    }

    @Override
    public TickThrottle getTickThrottle() {
        return tickThrottle;
    }

    @Override
    public MechanicLevel getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        boolean newLevel = this.level != null && level > this.level.lvl();
        this.level = MechanicLevel.from(this, level);

        if (newLevel) {
            onUpgrade(level);
        }
    }

    @Override
    public Location getLocation() {
        return loc.clone();
    }

    @Override
    public boolean canBeDeleted() {
        if (this instanceof Container<?> container && !container.isContainerEmpty()) {
            return false;
        } else if (this instanceof TransferCollection collection && !collection.isTransferEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public double getXP() {
        return StringUtil.formatDecimals(xp, 2);
    }

    @Override
    public BlockFace getRotation() {
        return rot;
    }

    @Override
    public Management getManagement() {
        return management;
    }

    @SuppressWarnings("unchecked")
    public <G extends BaseGui<G>> AtomicReference<G> getGuiInUse() {
        return (AtomicReference<G>) inUse;
    }

    @Override
    public String toString() {
        return getProfile().getName() + (getProfile().getLevelRegistry() != null ? " (Lvl " + level.lvl() + ")" : "");
    }
}
