package dk.superawesome.factories.mechanics;

import dk.superawesome.factories.gui.BaseGui;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public interface MechanicFactory<M extends Mechanic<M, ? extends BaseGui<?>>> {

    M create(Location loc, BlockFace rotation, MechanicStorageContext context);
}
