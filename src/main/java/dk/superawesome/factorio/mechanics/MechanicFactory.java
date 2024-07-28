package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.gui.BaseGui;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public interface MechanicFactory<M extends Mechanic<M, ? extends BaseGui<?>>> {

    M create(Location loc, BlockFace rotation, MechanicStorageContext context);
}
