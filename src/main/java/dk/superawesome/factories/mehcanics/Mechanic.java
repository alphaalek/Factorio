package dk.superawesome.factories.mehcanics;

import dk.superawesome.factories.gui.BaseGui;
import dk.superawesome.factories.items.ItemCollection;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface Mechanic<M extends Mechanic<M, G>, G extends BaseGui<G>> {

    Location getLocation();

    int getLevel();

    MechanicProfile<M, G> getProfile();

    void openInventory(Player player);

    void pipePut(ItemCollection collection);
}
