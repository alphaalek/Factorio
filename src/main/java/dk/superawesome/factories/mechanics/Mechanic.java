package dk.superawesome.factories.mechanics;

import dk.superawesome.factories.gui.BaseGui;
import dk.superawesome.factories.util.TickThrottle;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public interface Mechanic<M extends Mechanic<M, G>, G extends BaseGui<G>> {

    void blocksLoaded();

    TickThrottle getTickThrottle();

    Location getLocation();

    BlockFace getRotation();

    int getLevel();

    MechanicProfile<M, G> getProfile();

    void openInventory(Player player);
}
