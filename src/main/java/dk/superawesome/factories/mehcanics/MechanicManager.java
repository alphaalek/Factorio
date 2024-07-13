package dk.superawesome.factories.mehcanics;

import dk.superawesome.factories.mehcanics.pipes.events.PipePutEvent;
import dk.superawesome.factories.mehcanics.pipes.events.PipeSuckEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

public class MechanicManager implements Listener {

    private final Map<Location, Mechanic<?>> mechanics = new HashMap<>();

    public void load(MechanicProfile<?> profile, Location loc) {
        Mechanic<?> mechanic = profile.getFactory().create(loc);
        // TODO load from db
        mechanics.put(loc, mechanic);
    }

    public Mechanic<?> getNearbyMechanic(Location loc) {
        Location copy = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location rel = loc.add(x, y, z);
                    if (mechanics.containsKey(rel)) {
                        return mechanics.get(rel);
                    }
                    loc = copy;
                }
            }
        }

        return null;
    }

    public Mechanic<?> getMechanicAt(Location loc) {
        return mechanics.get(loc);
    }

    @EventHandler
    public void onPipeSuck(PipeSuckEvent event) {

    }

    @EventHandler
    public void onPipePut(PipePutEvent event) {
        Bukkit.getLogger().info("Pipe put " + event.getBlock());
    }
}
