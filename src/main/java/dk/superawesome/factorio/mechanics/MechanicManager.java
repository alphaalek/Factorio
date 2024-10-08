package dk.superawesome.factorio.mechanics;

import com.google.common.collect.Sets;
import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.api.events.MechanicBuildEvent;
import dk.superawesome.factorio.api.events.MechanicMoveEvent;
import dk.superawesome.factorio.api.events.MechanicLoadEvent;
import dk.superawesome.factorio.api.events.MechanicRemoveEvent;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipeSuckEvent;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.WorldGuard;
import dk.superawesome.factorio.util.db.Types;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
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

    public void loadMechanics() {
        for (Chunk chunk : world.getLoadedChunks()) {
            loadMechanics(chunk);
        }
    }

    public void loadMechanics(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof Sign
                    && (Tag.WALL_SIGNS.isTagged(state.getType()) || Tag.STANDING_SIGNS.isTagged(state.getType()))
                    && getProfileFrom((Sign) state).isPresent()) {
                // load this mechanic
                if (!loadMechanic((Sign) state)) {
                    // unable to load mechanic properly due to corrupt data
                    state.getBlock().setType(Material.AIR);
                }

                Routes.removeNearbyRoutes(state.getBlock());
            }
        }
    }

    public void handleThinking() {
        for (ThinkingMechanic thinking : thinkingMechanics) {
            if (thinking.getThinkDelayHandler().ready()) {
                thinking.think();
            }
        }
    }

    public Collection<Mechanic<?>> getAllMechanics() {
        return mechanics.values();
    }

    public Mechanic<?> load(MechanicProfile<?> profile, MechanicStorageContext context, Location loc, BlockFace rotation, boolean hasWallSign) {
        Mechanic<?> mechanic = profile.getFactory().create(loc, rotation, context, hasWallSign);
        if (mechanic instanceof ThinkingMechanic tm) {
            thinkingMechanics.add(tm);
        }

        for (Location part : Buildings.getLocations(mechanic)) {
            if (!mechanic.getBuilding().getSign(mechanic).getLocation().equals(part)) {
                this.mechanics.put(BlockUtil.getVec(part), mechanic);
            }
        }

        Bukkit.getPluginManager().registerEvents(mechanic, Factorio.get());

        return mechanic;
    }

    public void unload(Mechanic<?> mechanic) {
        for (Location loc : Buildings.getLocations(mechanic)) {
            mechanics.remove(BlockUtil.getVec(loc));
        }

        if (mechanic instanceof ThinkingMechanic) {
            thinkingMechanics.removeIf(m -> mechanic == m);
        }

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
                    BlockVector rel = new BlockVector(ori.clone().add(new Vector(x, y, z)));
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
        return getMechanicAt(BlockUtil.getVec(loc));
    }

    public Mechanic<?> getMechanicAt(BlockVector vec) {
        return mechanics.get(vec);
    }

    @EventHandler
    public void onPipeSuck(PipeSuckEvent event) {
        if (event.getBlock().getWorld().equals(this.world)) {
            Mechanic<?> mechanic = getMechanicAt(event.getLocation());
            if (mechanic instanceof TransferCollection tc) {
                event.setTransfer(tc);
            }
        }
    }

    @EventHandler
    public void onPipePut(PipePutEvent event) {
        if (event.getBlock().getWorld().equals(this.world)) {
            Mechanic<?> mechanic = getMechanicAt(event.getBlock().getLocation());
            if (mechanic instanceof Container<?> c && c.accepts(event.getTransfer()) && mechanic != event.getFrom()) {
                doTransfer(c, event.getTransfer(), event);
            }
        }
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        if (event.getWorld().equals(this.world)) {
            for (Mechanic<?> mechanic : new ArrayList<>(mechanics.values())) {
                mechanic.onUpdate();
            }

            Bukkit.getScheduler().runTaskAsynchronously(Factorio.get(), () -> {
                for (Mechanic<?> mechanic : new ArrayList<>(mechanics.values())) {
                    mechanic.save();
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private <C extends TransferCollection> void doTransfer(Container<? extends TransferCollection> container, TransferCollection collection, PipePutEvent event) {
        ((Container<C>)container).pipePut((C) collection, event);
    }

    public Optional<MechanicProfile<?>> getProfileFrom(Sign sign) {
        Optional<MechanicProfile<?>> profile = getProfileFrom(sign.getSide(Side.FRONT)::getLine, sign.getSide(Side.FRONT)::setLine);
        sign.update();
        return profile;
    }

    public Optional<MechanicProfile<?>> getProfileFrom(SignChangeEvent event) {
        return getProfileFrom(event::getLine, event::setLine);
    }

    public Optional<MechanicProfile<?>> getProfileFrom(Function<Integer, String> get, BiConsumer<Integer, String> set) {
        if (!get.apply(0).trim().startsWith("[")
                || !get.apply(0).trim().endsWith("]")) {
            return Optional.empty();
        }
        String type = get.apply(0).trim().substring(1, get.apply(0).trim().length() - 1);

        List<MechanicProfile<?>> match = Profiles.getProfiles()
                .stream()
                .filter(b -> b.getSignName().toLowerCase().startsWith(type.toLowerCase()))
                .toList();
        if (match.size() == 1) { // ensure only one possible match
            MechanicProfile<?> profile = match.get(0);

            // fix lowercase/uppercase and my headache
            set.accept(0, "[" + profile.getSignName() + "]");

            return Optional.of(profile);
        }

        return Optional.empty();
    }

    public boolean moveMechanic(Player player, Mechanic<?> mechanic, Location to, BlockFace rot, Block sign) {
        if (!Buildings.canMoveTo(to, rot, mechanic)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
            player.sendMessage("§cDer er ikke nok plads til at flytte maskinen!");
            return false;
        }

        MechanicMoveEvent event = new MechanicMoveEvent(player, mechanic, to, rot);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            player.sendMessage("§cDu kan ikke flytte maskinen hertil!");
            return false;
        }
        // check for WorldGuard access
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            for (Location loc : event.getLocations()) {
                if (!WorldGuard.canBuild(player, loc)) {
                    player.sendMessage("§cDu kan ikke flytte maskinen hertil!");
                    return false;
                }
            }
        }

        Routes.removeNearbyRoutes(mechanic.getLocation().getBlock());

        this.mechanics.remove(BlockUtil.getVec(mechanic.getLocation()));
        this.mechanics.put(BlockUtil.getVec(to), mechanic);

        mechanic.move(to, rot, sign);
        mechanic.onBlocksLoaded(player);

        // player stuff
        to.getWorld().playSound(to, Sound.BLOCK_ANVIL_PLACE, 0.375f, 1f);
        player.sendMessage("§eDu flyttede maskinen " + mechanic + " til " + Types.LOCATION.convert(to) + ".");

        return true;
    }

    public Block getBlockOn(Sign sign) {
        if (Tag.WALL_SIGNS.isTagged(sign.getType())) {
            return BlockUtil.getPointingBlock(sign.getBlock(), true);
        } else if (Tag.STANDING_SIGNS.isTagged(sign.getType())) {
             return sign.getBlock().getRelative(BlockFace.DOWN);
        } else {
            return null;
        }
    }

    public MechanicBuildResponse buildMechanic(Sign sign, Block on, Player owner) {
        Optional<MechanicProfile<?>> profile = getProfileFrom(sign);
        if (profile.isEmpty()) {
            return MechanicBuildResponse.NO_SUCH;
        }

        if (getMechanicPartially(on.getLocation()) != null) {
            return MechanicBuildResponse.ALREADY_EXISTS;
        }

        Mechanic<?> mechanic;
        try {
            BlockFace rotation = BlockUtil.getFacing(sign.getBlock());

            MechanicStorageContext context = contextProvider.create(on.getLocation(), rotation, profile.get().getName(), owner.getUniqueId());
            mechanic = loadMechanicFromSign(profile.get(), sign, on, rotation, context);
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
            if (!Buildings.checkCanBuild(mechanic)
                    || Tag.STANDING_SIGNS.isTagged(sign.getType()) && mechanic.getBuilding().deniesStandingSign()) {
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
        Buildings.build(sign.getWorld(), mechanic, Collections.singletonList(sign.getLocation()));
        mechanic.onBlocksLoaded(owner);

        Routes.removeNearbyRoutes(on);

        try {
            for (UUID defaultMember : Factorio.get().getMechanicController().getDefaultMembersFor(owner.getUniqueId())) {
                mechanic.getManagement().getMembers().add(defaultMember);
            }
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "A SQL error occurred!", ex);
            owner.sendMessage("§cDer opstod en fejl ved tilføjelse af standard medlemmer.");
        }

        MechanicLoadEvent postEvent = new MechanicLoadEvent(mechanic);
        Bukkit.getPluginManager().callEvent(postEvent);

        // play sound
        sign.getWorld().playSound(sign.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.475f, 1f);
        return MechanicBuildResponse.SUCCESS;
    }

    public boolean loadMechanic(Sign sign) {
        try {
            Optional<MechanicProfile<?>> profile = getProfileFrom(sign);
            Block on = getBlockOn(sign);
            if (on != null && profile.isPresent()) {
                // check if there is already a mechanic at this location
                if (getMechanicPartially(on.getLocation()) != null) {
                    return false;
                }

                // load the mechanic
                Mechanic<?> mechanic = loadMechanicFromSign(profile.get(), sign, on, BlockUtil.getFacing(sign.getBlock()), contextProvider.findAt(on.getLocation()));

                // ensure only standing signs for buildings that allow it
                if (Tag.STANDING_SIGNS.isTagged(sign.getType()) && mechanic.getBuilding().deniesStandingSign()) {
                    return false;
                }

                mechanic.onBlocksLoaded(null);

                MechanicLoadEvent postEvent = new MechanicLoadEvent(mechanic);
                Bukkit.getPluginManager().callEvent(postEvent);
            }
            return true;
        } catch (SQLException | IOException ex) {
            Factorio.get().getLogger().log(Level.SEVERE, "Failed to load mechanic at location " + sign.getLocation(), ex);
        }

        return false;
    }

    private Mechanic<?> loadMechanicFromSign(MechanicProfile<?> profile, Sign sign, Block on, BlockFace rotation, MechanicStorageContext context) throws IOException, SQLException {
        // load this mechanic
        Mechanic<?> mechanic = load(profile, context, on.getLocation(), rotation, Tag.WALL_SIGNS.isTagged(sign.getType()));

        if (mechanic instanceof AccessibleMechanic) {
            sign.getSide(Side.FRONT).setLine(1, "Lvl " + mechanic.getLevel().lvl());
            sign.update();
        }

        return mechanic;
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
        Buildings.remove(mechanic, mechanic.getLocation(), mechanic.getRotation(), true);
        Movement.removeMechanic(player, mechanic);

        Routes.removeNearbyRoutes(mechanic.getLocation().getBlock());

        // player stuff
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.6f);
        player.sendMessage("§eDu fjernede maskinen " + mechanic + " ved " + Types.LOCATION.convert(mechanic.getLocation()) + ".");
    }
}
