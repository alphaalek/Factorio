package dk.superawesome.factorio.mechanics;

import com.google.common.collect.Sets;
import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.api.events.MechanicBuildEvent;
import dk.superawesome.factorio.api.events.MechanicRemoveEvent;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipeSuckEvent;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.db.Query;
import dk.superawesome.factorio.util.db.Types;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class MechanicManager implements Listener {

    private final World world;
    private final MechanicStorageContext.Provider contextProvider;

    public MechanicManager(World world, MechanicStorageContext.Provider contextProvider) {
        this.world = world;
        this.contextProvider = contextProvider;

        Bukkit.getPluginManager().registerEvents(this, Factorio.get());

        Bukkit.getScheduler().runTaskTimer(Factorio.get(), this::handleThinking, 0L, 1L);
    }

    private final Map<BlockVector, Mechanic<?>> mechanics = new HashMap<>();
    private final Queue<ThinkingMechanic> thinkingMechanics = new LinkedList<>();

    public void handleThinking() {
        for (ThinkingMechanic thinking : thinkingMechanics) {
            if (!thinking.getTickThrottle().isThrottled() && thinking.getThinkDelayHandler().ready()) {
                thinking.think();
            }
        }
    }

    public Mechanic<?> load(MechanicProfile<?> profile, MechanicStorageContext context, Location loc, BlockFace rotation) {
        Mechanic<?> mechanic = profile.getFactory().create(loc, rotation, context);
        if (mechanic instanceof ThinkingMechanic) {
            thinkingMechanics.add((ThinkingMechanic) mechanic);
        }

        mechanics.put(BlockUtil.getVec(loc), mechanic);
        Bukkit.getPluginManager().registerEvents(mechanic, Factorio.get());

        return mechanic;
    }

    public void unload(Mechanic<?> mechanic) {
        // unregister this mechanic from the lists
        mechanics.remove(BlockUtil.getVec(mechanic.getLocation()));
        if (mechanic instanceof ThinkingMechanic) {
            thinkingMechanics.removeIf(m -> mechanic == m);
        }

        // finally unload this mechanic
        mechanic.unload();
        for (HandlerList list : HandlerList.getHandlerLists()) {
            list.unregister(mechanic);
        }
    }

    public void unloadMechanics(Chunk chunk) {
        // unload all mechanics in this chunk
        for (Mechanic<?> mechanic : Sets.newHashSet(mechanics.values())) {
            if (mechanic.getLocation().getChunk().equals(chunk)) {
                unload(mechanic);
            }
        }
    }

    public List<Mechanic<?>> getNearbyMechanics(Location loc) {
        List<Mechanic<?>> mechanics = new ArrayList<>();

        BlockVector ori = BlockUtil.getVec(loc);
        // iterate over the nearby blocks and check if there is any root mechanic block
        for (int x = -1; x <= 1; x++) {
            for (int y = -2; y <= 1; y++) {
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

    public Mechanic<?> getMechanicPartially(Location loc) {
        for (Mechanic<?> nearby : getNearbyMechanics(loc)) {
            if (Buildings.intersects(loc, nearby)) {
                return nearby;
            }
        }

        return null;
    }

    public Mechanic<?> getMechanicAt(Location loc) {
        return mechanics.get(BlockUtil.getVec(loc));
    }

    @EventHandler
    public void onPipeSuck(PipeSuckEvent event) {
        if (event.getBlock().getWorld().equals(this.world)) {
            Mechanic<?> mechanic = getMechanicAt(event.getBlock().getLocation());
            if (mechanic instanceof TransferCollection) {
                event.setTransfer((TransferCollection) mechanic);
            }
        }
    }

    @EventHandler
    public void onPipePut(PipePutEvent event) {
        if (event.getBlock().getWorld().equals(this.world)) {
            Mechanic<?> mechanic = getMechanicAt(event.getBlock().getLocation());
            if (mechanic instanceof Container && ((Container<?>)mechanic).accepts(event.getTransfer())) {
                doTransfer((Container<?>) mechanic, event.getTransfer(), event);
            }
        }
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        if (event.getWorld().equals(this.world)) {
            for (Mechanic<?> mechanic : mechanics.values()) {
                mechanic.save();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <C extends TransferCollection> void doTransfer(Container<? extends TransferCollection> container, TransferCollection collection, PipePutEvent event) {
        ((Container<C>)container).pipePut((C) collection, event);
    }

    public Optional<MechanicProfile<?>> getProfileFrom(Sign sign) {
        if (!sign.getSide(Side.FRONT).getLine(0).trim().startsWith("[")
                || !sign.getSide(Side.FRONT).getLine(0).trim().endsWith("]")) {
            return Optional.empty();
        }
        String type = sign.getSide(Side.FRONT).getLine(0).trim().substring(1, sign.getSide(Side.FRONT).getLine(0).trim().length() - 1);

        Optional<MechanicProfile<?>> profile = Profiles.getProfiles()
                .stream()
                .filter(b -> b.getName().equalsIgnoreCase(type))
                .findFirst();
        if (profile.isPresent()) {
            // fix lowercase/uppercase and my headache
            sign.getSide(Side.FRONT).setLine(0, "[" + profile.get().getName() + "]");
            sign.update();
        }

        return profile;
    }

    public MechanicBuildResponse buildMechanic(Sign sign, Player owner) {
        Optional<MechanicProfile<?>> profile = getProfileFrom(sign);
        if (profile.isEmpty()) {
            return MechanicBuildResponse.NO_SUCH;
        }

        Block pointing = BlockUtil.getPointingBlock(sign.getBlock(), true);
        if (pointing != null && getMechanicPartially(pointing.getLocation()) != null) {
            return MechanicBuildResponse.ALREADY_EXISTS;
        }

        Mechanic<?> mechanic;
        try {
            BlockFace rotation = ((org.bukkit.block.data.type.WallSign)sign.getBlockData()).getFacing();
            mechanic = loadMechanicFromSign(profile.get(), sign, (type, on) -> contextProvider.create(on.getLocation(), rotation, type, owner.getUniqueId()));
            if (mechanic == null) {
                return MechanicBuildResponse.NO_SUCH;
            }
        } catch (SQLException | IOException ex) {
            Factorio.get().getLogger().log(Level.SEVERE, "Failed to create mechanic at location " + sign.getLocation(), ex);
            return MechanicBuildResponse.ERROR;
        }

        MechanicBuildEvent event = new MechanicBuildEvent(owner, mechanic);
        verify: {
            MechanicBuildResponse response;
            if (!Buildings.checkCanBuild(mechanic)) {
                response = MechanicBuildResponse.NOT_PLACED_BLOCKS;
            } else if (!Buildings.hasSpaceFor(sign.getBlock(), mechanic)) {
                response = MechanicBuildResponse.NOT_ENOUGH_SPACE;
            } else {
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    response = MechanicBuildResponse.ABORT;
                } else {
                    break verify;
                }
            }

            unload(mechanic);
            return response;
        }

        // place the blocks for this mechanic
        Buildings.build(sign.getWorld(), mechanic);
        mechanic.onBlocksLoaded();

        // play sound
        sign.getWorld().playSound(sign.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.675f, 1f);
        return MechanicBuildResponse.SUCCESS;
    }

    public void loadMechanic(Sign sign) {
        try {
            Optional<MechanicProfile<?>> profile = getProfileFrom(sign);
            if (profile.isPresent()) {
                Mechanic<?> mechanic = loadMechanicFromSign(profile.get(), sign, (__, on) -> contextProvider.findAt(on.getLocation()));
                if (mechanic != null) {
                    mechanic.onBlocksLoaded();
                }
            }
        } catch (SQLException | IOException ex) {
            Factorio.get().getLogger().log(Level.SEVERE, "Failed to load mechanic at location " + sign.getLocation(), ex);
        }
    }

    private Mechanic<?> loadMechanicFromSign(MechanicProfile<?> profile, Sign sign, Query.CheckedBiFunction<String, Block, MechanicStorageContext> context) throws IOException, SQLException {
        // get the block which the sign is hanging on, because this block is the root of the mechanic
        Block on = BlockUtil.getPointingBlock(sign.getBlock(), true);
        if (on == null) {
            return null;
        }

        // load this mechanic
        BlockFace rotation = ((org.bukkit.block.data.type.WallSign)sign.getBlockData()).getFacing();
        return load(profile, context.<SQLException>sneaky(profile.getName(), on), on.getLocation(), rotation);
    }

    public void removeMechanic(Player player, Mechanic<?> mechanic) {
        // call mechanic remove event to event handlers
        MechanicRemoveEvent removeEvent = new MechanicRemoveEvent(player, mechanic);
        Bukkit.getPluginManager().callEvent(removeEvent);
        if (removeEvent.isCancelled()) {
            // this event was cancelled. (why though?)
            return;
        }

        // unload and delete this mechanic
        unload(mechanic);
        try {
            Factorio.get().getContextProvider().deleteAt(mechanic.getLocation());
        } catch (SQLException ex) {
            player.sendMessage("§cDer opstod en fejl! Kontakt en udvikler.");
            Factorio.get().getLogger().log(Level.SEVERE, "Failed to delete mechanic at location " + mechanic.getLocation(), ex);
            return;
        }
        Buildings.remove(player.getWorld(), mechanic);

        // player stuff
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.6f);
        player.sendMessage("§eDu fjernede maskinen " + mechanic.toString() + " ved " + Types.LOCATION.convert(mechanic.getLocation()) + ".");
    }
}
