package dk.superawesome.factories.mehcanics;

import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.util.TickThrottle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public abstract class AbstractMechanic<M extends Mechanic<M>> implements Mechanic<M>, ItemCollection {

    private final TickThrottle tickThrottle = new TickThrottle();
    private final Location loc;

    public AbstractMechanic(Location loc) {
        this.loc = loc;
    }

    public TickThrottle getTickThrottle() {
        return tickThrottle;
    }

    @Override
    public int getLevel() {
        return 1;
    }

    @Override
    public Location getLocation() {
        return loc;
    }

    @Override
    public boolean has(ItemStack stack) {
        return false;
    }

    @Override
    public List<ItemStack> take(ItemStack stack) {
        Bukkit.getLogger().info("Takes " + stack);

        return Collections.EMPTY_LIST;
    }
}
