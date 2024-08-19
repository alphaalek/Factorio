package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.util.TickThrottle;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.concurrent.atomic.AtomicReference;

public interface Mechanic<M extends Mechanic<M>> extends Listener {

    void unload();

    void save();

    void onBlocksLoaded(Player by);

    boolean exists();

    boolean canBeDeleted();

    TickThrottle getTickThrottle();

    Location getLocation();

    BlockFace getRotation();

    int getLevel();

    Management getManagement();

    MechanicProfile<M> getProfile();

    <G extends BaseGui<G>> AtomicReference<G> getGuiInUse();
}
