package dk.superawesome.factories.mechanics;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.building.Buildings;
import dk.superawesome.factories.mechanics.routes.events.PipePutEvent;
import dk.superawesome.factories.mechanics.routes.events.PipeSuckEvent;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.util.*;

public class MechanicManager implements Listener {

    private final World world;

    public MechanicManager(World world) {
        this.world = world;

        Bukkit.getPluginManager().registerEvents(this, Factories.get());

        Bukkit.getScheduler().runTaskTimer(Factories.get(), this::handleThinking, 0L, 1L);
    }

    private final Map<BlockVector, Mechanic<?, ?>> mechanics = new HashMap<>();
    private final Queue<ThinkingMechanic> thinkingMechanics = new LinkedList<>();

    public void handleThinking() {
        for (ThinkingMechanic thinking : thinkingMechanics) {
            if (!thinking.getTickThrottle().isThrottled() && thinking.getDelayHandler().ready()) {
                thinking.think();
            }
        }
    }

    public Mechanic<?, ?> load(MechanicProfile<?, ?> profile, Location loc, BlockFace rotation) {
        Mechanic<?, ?> mechanic = profile.getFactory().create(loc, rotation);
        if (mechanic instanceof ThinkingMechanic) {
            thinkingMechanics.add((ThinkingMechanic) mechanic);
        }

        mechanics.put(BlockUtil.getVec(loc), mechanic);

        return mechanic;
    }

    public void unload(Mechanic<?, ?> mechanic) {
        mechanics.remove(BlockUtil.getVec(mechanic.getLocation()));

        if (mechanic instanceof ThinkingMechanic) {
            thinkingMechanics.removeIf(m -> mechanic == m);
        }
    }

    public List<Mechanic<?, ?>> getNearbyMechanics(Location loc) {
        List<Mechanic<?, ?>> mechanics = new ArrayList<>();

        BlockVector ori = BlockUtil.getVec(loc);
        // iterate over the nearby blocks and check if there is any root mechanic block
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockVector rel = (BlockVector) new BlockVector(ori).add(new Vector(x, y, z));
                    if (this.mechanics.containsKey(rel)) {
                        mechanics.add(this.mechanics.get(rel));
                    }
                }
            }
        }

        return mechanics;
    }

    public Mechanic<?, ?> getMechanicPartially(Location loc) {
        for (Mechanic<?, ?> nearby : getNearbyMechanics(loc)) {
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
            if (mechanic instanceof Container) {
                ((Container)mechanic).pipePut(event.getItems());
            }
        }
    }

    public void buildMechanic(Sign sign) {
        Mechanic<?, ?> mechanic = loadMechanicFromSign(sign);
        if (mechanic == null) {
            return;
        }

        // check if we can build this mechanic in the world
        if (!Buildings.hasSpaceFor(sign.getWorld(), sign.getBlock(), mechanic)) {
            unload(mechanic);
            return;
        }

        // place the blocks for this mechanic
        Buildings.build(sign.getWorld(), mechanic);
        mechanic.blocksLoaded();

        // world stuff
        sign.getWorld().playSound(sign.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.8f);
        sign.getWorld().playSound(sign.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 0.9f, 1f);
    }

    public void loadMechanic(Sign sign) {
        Mechanic<?, ?> mechanic = loadMechanicFromSign(sign);
        if (mechanic != null) {
            // TODO db load

            mechanic.blocksLoaded();
        }
    }

    private Mechanic<?, ?> loadMechanicFromSign(Sign sign) {
        // check if this sign is related to a mechanic
        if (!sign.getSide(Side.FRONT).getLine(0).startsWith("[")
                || !sign.getSide(Side.FRONT).getLine(0).endsWith("]")) {
            return null;
        }
        String type = sign.getSide(Side.FRONT).getLine(0).substring(1, sign.getSide(Side.FRONT).getLine(0).length() - 1);

        Optional<MechanicProfile<?, ?>> mechanicProfile = Profiles.getProfiles()
                .stream()
                .filter(b -> b.getName().equalsIgnoreCase(type))
                .findFirst();
        if (!mechanicProfile.isPresent()) {
            return null;
        }
        // fix lowercase/uppercase and my headache
        sign.getSide(Side.FRONT).setLine(0, "[" + mechanicProfile.get().getName() + "]");

        // get the block which the sign is hanging on, because this block is the root of the mechanic
        Block on = BlockUtil.getPointingBlock(sign.getBlock(), true);
        if (on == null) {
            return null;
        }

        // load this mechanic
        BlockFace rotation = ((org.bukkit.block.data.type.WallSign)sign.getBlockData()).getFacing();
        return load(mechanicProfile.get(), on.getLocation(), rotation);
    }
}
