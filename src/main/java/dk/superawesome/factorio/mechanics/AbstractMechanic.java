package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.util.TickThrottle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public abstract class AbstractMechanic<M extends Mechanic<M, G>, G extends BaseGui<G>> implements Mechanic<M, G> {

    protected final AtomicReference<G> inUse = new AtomicReference<>();
    protected final TickThrottle tickThrottle = new TickThrottle();
    protected final Location loc;
    protected final BlockFace rotation;
    protected final MechanicLevel level;
    protected final MechanicStorageContext context;
    protected final Management management;

    public AbstractMechanic(Location loc, BlockFace rotation, MechanicStorageContext context) {
        this.loc = loc;
        this.rotation = rotation;
        this.context = context;

        try {
            this.level = MechanicLevel.from(this, this.context.getLevel());
            this.management = this.context.getManagement();
        } catch (SQLException | IOException ex) {
            throw new RuntimeException("Failed to acquire data of mechanic at location " + loc, ex);
        }
    }

    protected void loadFromStorage() {
        try  {
            if (context.hasContext()) {
                load(context);
            }
        } catch (Exception ex) {
            Factorio.get().getLogger().log(Level.SEVERE, "Failed to load mechanic " + getProfile().getName()  + ", " + getLocation(), ex);
        }
    }

    @Override
    public void unload() {
        try  {
            this.context.getController().setLevel(this.loc, this.level.getLevel());
            if (this.management != Management.ALL_ACCESS) {
                this.context.uploadManagement(this.management);
            }

            save(this.context);
        } catch (Exception ex) {
            Factorio.get().getLogger().log(Level.SEVERE, "Failed to save mechanic " + getProfile().getName()  + ", " + getLocation(), ex);
        }
    }

    public void load(MechanicStorageContext context) throws Exception {
        // to be overridden if needed
    }

    public void save(MechanicStorageContext context) throws Exception {
        // to be overridden if needed
    }

    @Override
    public void blocksLoaded() {

    }

    @Override
    public TickThrottle getTickThrottle() {
        return tickThrottle;
    }

    @Override
    public int getLevel() {
        return 1;
    }

    @Override
    public Location getLocation() {
        return loc;
    }

    @Override
    public BlockFace getRotation() {
        return rotation;
    }

    @Override
    public Management getManagement() {
        return management;
    }

    @Override
    public void openInventory(Player player) {
        G inUse = this.inUse.get();
        if (inUse != null) {
            player.openInventory(inUse.getInventory());
            return;
        }

        G gui = getProfile().getGuiFactory().create((M) this, this.inUse);
        player.openInventory(gui.getInventory());
    }
}
