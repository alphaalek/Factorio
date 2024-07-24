package dk.superawesome.factories.mechanics;

import dk.superawesome.factories.gui.BaseGui;
import dk.superawesome.factories.util.TickThrottle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;

public abstract class AbstractMechanic<M extends Mechanic<M, G>, G extends BaseGui<G>> implements Mechanic<M, G> {

    protected final AtomicReference<G> inUse = new AtomicReference<>();
    protected final TickThrottle tickThrottle = new TickThrottle();
    protected final Location loc;
    protected final BlockFace rotation;
    protected final MechanicLevel level;
    protected final MechanicStorageContext context;

    public AbstractMechanic(Location loc, BlockFace rotation, MechanicStorageContext context) {
        this.loc = loc;
        this.rotation = rotation;
        this.context = context;
        this.level = MechanicLevel.from(this, context.getLevel());

        try  {
            load(context);
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to load mechanic data " + getProfile().getName()  + ", " + getLocation(), ex);
        }
    }

    public void load(MechanicStorageContext context) throws Exception {
        // to be overridden if needed
    }

    public void save(MechanicStorageContext context) throws Exception {
        // to be overridden if needed
    }

    @Override
    public void unload() {
        try  {
            save(context);
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save mechanic data " + getProfile().getName()  + ", " + getLocation(), ex);
        }
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

    protected interface Updater<T> {

        T get();

        void set(T val);
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
