package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.util.TickThrottle;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.concurrent.atomic.AtomicReference;

public interface Mechanic<M extends Mechanic<M>> extends Listener, Source {

    void unload();

    void save();

    void move(Location loc, BlockFace rot, Block sign);

    void onUpgrade(int newLevel);

    void onBlocksLoaded(Player by);

    boolean exists();

    boolean canBeDeleted();

    double getXP();

    TickThrottle getTickThrottle();

    Location getLocation();

    BlockFace getRotation();

    MechanicLevel getLevel();

    void setLevel(int level);

    Management getManagement();

    MechanicProfile<M> getProfile();

    <G extends BaseGui<G>> AtomicReference<G> getGuiInUse();
}
