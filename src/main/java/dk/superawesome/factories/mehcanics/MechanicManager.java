package dk.superawesome.factories.mehcanics;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.building.Buildings;
import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.mehcanics.pipes.events.PipePutEvent;
import dk.superawesome.factories.mehcanics.pipes.events.PipeSuckEvent;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class MechanicManager implements Listener {

    private final World world;

    public MechanicManager(World world) {
        this.world = world;

        Bukkit.getPluginManager().registerEvents(this, Factories.get());
    }

    private final Map<BlockVector, Mechanic<?, ?>> mechanics = new HashMap<>();

    public void load(MechanicProfile<?, ?> profile, Location loc) {
        Mechanic<?, ?> mechanic = profile.getFactory().create(loc);
        // TODO load from db
        mechanics.put(BlockUtil.getVec(loc), mechanic);
    }

    public Mechanic<?, ?> getNearbyMechanic(Location loc) {

        BlockVector ori = BlockUtil.getVec(loc);

        // iterate over the nearby blocks and check if there is any root mechanic block
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockVector rel = (BlockVector) new BlockVector(ori).add(new Vector(x, y, z));
                    if (mechanics.containsKey(rel)) {
                        return mechanics.get(rel);
                    }
                }
            }
        }

        return null;
    }

    public Mechanic<?, ?> getMechanicPartially(Location loc) {
        Mechanic<?, ?> nearby = getNearbyMechanic(loc);
        if (nearby != null) {
            if (Buildings.intersects(loc, nearby)) {
                return nearby;
            }
        }

        return null;
    }

    public Mechanic<?, ?> getMechanicAt(Location loc) {
        return mechanics.get(BlockUtil.getVec(loc));
    }

    @EventHandler
    public void onPipeSuck(PipeSuckEvent event) {
        if (event.getBlock().getWorld().equals(this.world)) {
            Mechanic<?, ?> mechanic = getMechanicAt(event.getBlock().getLocation());
            if (mechanic instanceof ItemCollection) {
                event.setItems((ItemCollection) mechanic);
            }
        }
    }

    @EventHandler
    public void onPipePut(PipePutEvent event) {
        if (event.getBlock().getWorld().equals(this.world)) {
            Mechanic<?, ?> mechanic = getMechanicAt(event.getBlock().getLocation());
            if (mechanic != null) {
                mechanic.pipePut(event.getItems());
            }
        }
    }
}
