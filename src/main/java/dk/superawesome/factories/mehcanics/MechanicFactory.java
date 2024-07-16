package dk.superawesome.factories.mehcanics;

import dk.superawesome.factories.gui.BaseGui;
import org.bukkit.Location;

public interface MechanicFactory<M extends Mechanic<M, ? extends BaseGui<?>>> {

    M create(Location loc);
}
