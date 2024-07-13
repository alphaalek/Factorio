package dk.superawesome.factories.mehcanics;

import dk.superawesome.factories.items.ItemCollection;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface Mechanic<M extends Mechanic<M>> {

    Location getLocation();

    int getLevel();

    MechanicProfile<M> getProfile();

    void openInventory(Player player);

    void pipePut(ItemCollection collection);
}
