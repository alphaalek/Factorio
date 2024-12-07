package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.util.TickThrottle;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public interface Mechanic<M extends Mechanic<M>> extends Listener, Source {

    default Building getBuilding() {
        return getProfile().getBuilding(hasWallSign());
    }

    boolean unload();

    boolean save();

    void move(Location loc, BlockFace rot, Block sign);

    void onUpgrade(int newLevel);

    void onUpdate();

    void onBlocksLoaded(Player by);

    boolean exists();

    boolean canBeDeleted();

    boolean hasWallSign();

    void setXP(double xp);

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
