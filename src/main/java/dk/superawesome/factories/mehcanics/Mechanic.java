package dk.superawesome.factories.mehcanics;

import dk.superawesome.factories.items.ItemCollection;
import org.bukkit.Location;

public interface Mechanic<M extends Mechanic<M>> {

    Location getLocation();

    MechanicProfile<M> getProfile();

    void pipePut(ItemCollection collection);
}
