package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.mechanics.db.StorageException;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.TickThrottle;
import dk.superawesome.factorio.util.db.Types;
import dk.superawesome.factorio.util.statics.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public abstract class AbstractMechanic<M extends Mechanic<M>> implements Mechanic<M> {

    protected final AtomicReference<? extends BaseGui<?>> inUse = new AtomicReference<>();
    protected final TickThrottle tickThrottle = new TickThrottle();
    protected final MechanicStorageContext context;
    protected final Management management;
    protected final boolean hasWallSign;

    protected Snapshot lastSnapshot;
    protected Location loc;
    protected BlockFace rot;

    protected MechanicLevel level;
    protected double xp;
    protected boolean exists = true;

    public AbstractMechanic(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        this.loc = loc;
        this.rot = rotation;
        this.context = context;
        this.hasWallSign = hasWallSign;

        if (!isBuild) {
            try {
                this.lastSnapshot = context.load();

                this.management = this.lastSnapshot.management();
                this.xp = this.lastSnapshot.xp();
                this.level = MechanicLevel.from(this, this.lastSnapshot.level());
            } catch (SQLException | IOException ex) {
                throw new RuntimeException("Failed to load mechanic " + this  + " at " + Types.LOCATION.convert(loc), ex);
            }

            try {
                loadData(MechanicStorageContext.decode(this.lastSnapshot.strData()));
            } catch (Exception ex) {
                Factorio.get().getLogger().log(Level.SEVERE, "Failed to load data for mechanic " + this  + " at " + getLocation(), ex);
            }
        } else {
            this.level = MechanicLevel.from(this, 1);
            this.management = context.getFallbackManagement();

            try {
                Factorio.get().getContextProvider().create(this.loc, this.rot, getProfile().getName(), this.management.getOwner());
            } catch (StorageException ex) {
                throw new RuntimeException("Failed to initialize mechanic " + this  + " at " + Types.LOCATION.convert(loc), ex);
            }

            this.lastSnapshot = new Snapshot(1, 0, this.management.copy(), "");
        }
    }

    @Override
    public boolean unload() {
        exists = false;
        return save();
    }

    @Override
    public boolean save() {
        try {
            // save data for this mechanic
            Snapshot snapshot = Snapshot.create(this);
            if (!snapshot.equals(this.lastSnapshot)) {
                this.lastSnapshot = snapshot;
                this.context.save(this.lastSnapshot);
            }

            return true;
        } catch (Exception ex) {
            Factorio.get().getLogger().log(Level.SEVERE, "Failed to save mechanic " + this + ", " + getLocation(), ex);
            return false;
        }
    }

    @Override
    public void move(Location loc, BlockFace rot, Block signBlock) {
        Location prevLoc = this.loc;
        BlockFace prevRot = this.rot;

        this.loc = loc;
        this.rot = rot;

        // change the stored location and rotation data
        try {
            this.context.move(loc, rot);
        } catch (Exception ex) {
            Factorio.get().getLogger().log(Level.SEVERE, "Failed to move mechanic " + this + ", " + getLocation(), ex);
        }

        Sign prevSign = (Sign) signBlock.getState();
        String[] lines = new String[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = prevSign.getSide(Side.FRONT).getLine(i);
        }

        // place the blocks for the mechanic at the new place
        Buildings.copy(loc.getWorld(), prevLoc.getWorld(), prevLoc, prevRot, this.loc, this.rot, this);

        // transfer sign lines
        Sign sign = getSign();
        for (int i = 0; i < 4; i++) {
            sign.getSide(Side.FRONT).setLine(i, lines[i]);
        }
        sign.update();

        // remove the previous blocks
        Buildings.destroy(this, prevLoc, prevRot, Buildings.getLocations(this));
    }

    @Override
    public void onUpdate() {

    }

    @Override
    public boolean exists() {
        return exists;
    }

    public void loadData(ByteArrayInputStream data) throws Exception {
        // to be overridden if needed
    }

    public Optional<ByteArrayOutputStream> saveData() throws Exception {
        // to be overridden if needed
        return Optional.empty();
    }

    protected Sign getSign() {
        try {
            return (Sign) getBuilding().getSign(this).getState();
        } catch (ClassCastException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Sign block is not sign: " + this.loc, ex);
            return null;
        }
    }

    @Override
    public void onUpgrade(int newLevel) {
        Sign sign = getSign();
        sign.getSide(Side.FRONT).setLine(1, "Lvl " + newLevel);
        sign.update();
    }

    @Override
    public void onBlocksLoaded(Player by) {
        onUpdate();
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
    public void setXP(double xp) {
        this.xp = xp;
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
    public boolean hasWallSign() {
        return hasWallSign;
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
