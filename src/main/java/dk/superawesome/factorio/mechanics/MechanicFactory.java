package dk.superawesome.factorio.mechanics;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public interface MechanicFactory<M extends Mechanic<M>> {

    M create(Location loc, BlockFace rotation, MechanicStorageContext context);
}
