package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.util.TickThrottle;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public interface Mechanic<M extends Mechanic<M, G>, G extends BaseGui<G>> {

    void unload();

    void blocksLoaded();

    TickThrottle getTickThrottle();

    Location getLocation();

    BlockFace getRotation();

    int getLevel();

    Management getManagement();

    MechanicProfile<M, G> getProfile();

    void openInventory(Player player);
}