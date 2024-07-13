package dk.superawesome.factories.mehcanics;

import org.bukkit.Location;

public interface MechanicFactory<M extends Mechanic<M>> {

    M create(Location loc);
}
