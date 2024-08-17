package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.util.TickThrottle;
import dk.superawesome.factorio.util.db.Types;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public abstract class AbstractMechanic<M extends Mechanic<M>> implements Mechanic<M> {

    protected final AtomicReference<? extends BaseGui<?>> inUse = new AtomicReference<>();
    protected final TickThrottle tickThrottle = new TickThrottle();
    protected final Location loc;
    protected final BlockFace rot;
    protected final MechanicLevel level;
    protected final MechanicStorageContext context;
    protected final Management management;

    private boolean exists = true;

    public AbstractMechanic(Location loc, BlockFace rotation, MechanicStorageContext context) {
        this.loc = loc;
        this.rot = rotation;
        this.context = context;

        try {
            this.level = MechanicLevel.from(this, context.getLevel());
            this.management = context.getManagement();
        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Failed to load mechanic " + this  + " at " + Types.LOCATION.convert(loc), ex);
        }
    }

    protected void loadFromStorage() {
        try  {
            if (this.context.hasContext()) {
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
            // ensure record exists
            if (!this.context.hasContext()) {
                Factorio.get().getContextProvider().create(this.loc, this.rot, getProfile().getName(), this.management.getOwner());
            }

            // save data for this mechanic
            this.context.getController().setLevel(this.loc, this.level.getLevel());
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

    @Override
    public void onBlocksLoaded() {

    }

    @Override
    public TickThrottle getTickThrottle() {
        return tickThrottle;
    }

    @Override
    public int getLevel() {
        return level.getLevel();
    }

    @Override
    public Location getLocation() {
        return loc.clone();
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
        return getProfile().getName() + (getProfile().getLevelRegistry() != null ? " (Lvl " + getLevel() + ")" : "");
    }
}
