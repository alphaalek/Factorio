package dk.superawesome.factorio.mechanics;

import com.google.common.collect.Sets;
import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.api.events.*;
import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.db.StorageException;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipeSuckEvent;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.WorldGuard;
import dk.superawesome.factorio.util.db.Query;
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

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.*;
import java.util.logging.Level;

public class MechanicManager implements Listener {

    private static final ExecutorService LOADING_THREAD = Executors.newSingleThreadExecutor();
    private final World world;
    private final MechanicStorageContext.Provider contextProvider;

    public MechanicManager(World world, MechanicStorageContext.Provider contextProvider) {
        this.world = world;
        this.contextProvider = contextProvider;

        Bukkit.getPluginManager().registerEvents(this, Factorio.get());

        Bukkit.getScheduler().runTaskTimer(Factorio.get(), this::handleThinking, 0L, 1L);
    }

    private final Map<BlockVector, Mechanic<?>> mechanics = new HashMap<>();
    private final Map<BlockVector, MechanicProfile<?>> loadingMechanics = new HashMap<>();
    private final Queue<ThinkingMechanic> thinkingMechanics = new LinkedList<>();

    public void loadMechanics() {
        for (Chunk chunk : world.getLoadedChunks()) {
            loadMechanics(chunk);
        }
    }

    public void loadMechanics(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            BlockVector vec = BlockUtil.getVec(state.getLocation());
            if (!mechanics.containsKey(vec) && !loadingMechanics.containsKey(vec)
                    && state instanceof Sign sign
                    && (Tag.WALL_SIGNS.isTagged(state.getType()) || Tag.STANDING_SIGNS.isTagged(state.getType()))) {

                Optional<MechanicProfile<?>> profile = getProfileFrom(sign);
                if (profile.isPresent()) {
                    // add this profile to loading mechanics
                    Building building = profile.get().getBuilding(Tag.WALL_SIGNS.isTagged(state.getType()));
                    List<Location> locations = Buildings.getLocations(building, getBlockOn(sign).getLocation(), BlockUtil.getFacing(state.getBlock()));
                    for (Location loc : locations) {
                        loadingMechanics.put(BlockUtil.getVec(loc), profile.get());
                    }

                    // load this mechanic
                    loadMechanic(sign, loaded -> {
                        if (!loaded) {
                            // unable to load mechanic properly due to corrupt data
                            state.getBlock().setType(Material.AIR);

                            Mechanic<?> mechanic = getMechanicAt(state.getLocation());
                            if (mechanic != null) {
                                deleteMechanic(mechanic);
                            }
                        }

                        Routes.removeNearbyRoutes(state.getBlock());

                        // remove from loading mechanics
                        for (Location loc : locations) {
                            loadingMechanics.remove(BlockUtil.getVec(loc));
                        }
                    });
                }
            }
        }
    }

    public void handleThinking() {
        for (ThinkingMechanic thinking : this.thinkingMechanics) {
            if (thinking.getThinkDelayHandler().ready()) {
                thinking.think();
            }
        }
    }

    public Collection<Mechanic<?>> getAllMechanics() {
        return mechanics.values();
    }

    public void load(MechanicProfile<?> profile, Query.CheckedSupplier<MechanicStorageContext, StorageException> contextSupplier, Location loc, BlockFace rotation, boolean hasWallSign, boolean isBuild, Consumer<Mechanic<?>> callback) {
        LOADING_THREAD.submit(() -> {
            try {
                Mechanic<?> mechanic = profile.getFactory().create(loc, rotation, contextSupplier.get(), hasWallSign, isBuild);

                Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                    if (mechanic instanceof ThinkingMechanic tm) {
                        this.thinkingMechanics.add(tm);
                    }

                    for (Location part : Buildings.getLocations(mechanic)) {
                        this.mechanics.put(BlockUtil.getVec(part), mechanic);
                    }

                    Bukkit.getPluginManager().registerEvents(mechanic, Factorio.get());

                    callback.accept(mechanic);
                });
            } catch (StorageException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public void unregister(Mechanic<?> mechanic) {
        for (Location loc : Buildings.getLocations(mechanic)) {
            this.mechanics.remove(BlockUtil.getVec(loc));
        }

        if (mechanic instanceof ThinkingMechanic) {
            this.thinkingMechanics.removeIf(m -> mechanic == m);
        }

        for (HandlerList list : HandlerList.getHandlerLists()) {
            list.unregister(mechanic);
        }
    }

    public void unload(Mechanic<?> mechanic, boolean async) {
        unregister(mechanic);

        if (async) {
            LOADING_THREAD.submit(mechanic::unload);
        } else {
            mechanic.unload();
        }
    }

    public void unload(Mechanic<?> mechanic) {
        unload(mechanic, true);
    }

    public void unloadMechanics(Chunk chunk, boolean async) {
        // unload all mechanics in this chunk
        for (Mechanic<?> mechanic : Sets.newHashSet(mechanics.values())) {
            if (mechanic.getLocation().getChunk().equals(chunk)) {
                unload(mechanic, async);
            }
        }
    }

    public MechanicProfile<?> getLoadingMechanic(Location loc) {
        return loadingMechanics.get(BlockUtil.getVec(loc));
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
                try {
                    mechanic.onUpdate();
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "Error updating mechanic: " + mechanic.getLocation(), ex);
                }
            }

            Bukkit.getScheduler().runTaskAsynchronously(Factorio.get(), () -> {
                for (Mechanic<?> mechanic : new ArrayList<>(mechanics.values())) {
                    try {
                        mechanic.save();
                    } catch (Exception ex) {
                        Bukkit.getLogger().log(Level.SEVERE, "Error saving mechanic: " + mechanic.getLocation(), ex);
                    }
                }
            });
        }
    }

    @EventHandler
    public void onOwnerChange(MechanicChangeOwnerEvent event) {
        if (this.world.equals(event.getMechanic().getLocation().getWorld())) {
            if (event.getPlayer() == null || event.getNewOwner() == null) {
                event.setCancelled(true);
                return;
            }
            event.getMechanic().getManagement().setOwner(event.getNewOwner());
            event.getMechanic().getManagement().getMembers().clear();
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

        // check for WorldGuard access
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            for (Location loc : Buildings.getLocations(mechanic.getBuilding(), to, rot)) {
                if (!WorldGuard.canBuild(player, loc)) {
                    player.sendMessage("§cDu kan ikke flytte maskinen hertil!");
                    return false;
                }
            }
        }

        MechanicMoveEvent event = new MechanicMoveEvent(player, mechanic, to, rot);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            player.sendMessage("§cDu kan ikke flytte maskinen hertil!");
            return false;
        }

        Routes.removeNearbyRoutes(mechanic.getLocation().getBlock());

        for (Location loc : Buildings.getLocations(mechanic)) {
            this.mechanics.remove(BlockUtil.getVec(loc));
        }

        mechanic.move(to, rot, sign);
        mechanic.onBlocksLoaded(player);

        for (Location loc : Buildings.getLocations(mechanic)) {
            this.mechanics.put(BlockUtil.getVec(loc), mechanic);
        }

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

    public void buildMechanic(Sign sign, Block on, Player owner, Consumer<MechanicBuildResponse> callback) {
        try {
            buildMechanic0(sign, on, owner, callback);
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to build mechanic at " + sign.getBlock().getLocation(), ex);
            callback.accept(MechanicBuildResponse.ERROR);
        }
    }

    private void buildMechanic0(Sign sign, Block on, Player owner, Consumer<MechanicBuildResponse> callback) {
        Optional<MechanicProfile<?>> profile = getProfileFrom(sign);
        if (profile.isEmpty()) {
            callback.accept(MechanicBuildResponse.NO_SUCH);
            return;
        }

        Mechanic<?> at = getMechanicAt(on.getLocation());
        if (at != null && !at.getBuilding().getSign(at).getLocation().equals(sign.getLocation())) {
            callback.accept(MechanicBuildResponse.ALREADY_EXISTS);
            return;
        }

        BlockFace rotation = BlockUtil.getFacing(sign.getBlock());

        Query.CheckedSupplier<MechanicStorageContext, StorageException> context = () -> {
            // delete any previous mechanics on this location
            contextProvider.deleteAt(on.getLocation());

            // create context for this mechanic
            return contextProvider.create(on.getLocation(), rotation, profile.get().getName(), owner.getUniqueId());
        };
        loadMechanicFromSign(profile.get(), context, sign, on, rotation, true, mechanic -> {
            if (mechanic == null) {
                callback.accept(MechanicBuildResponse.NO_SUCH);
                return;
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
                callback.accept(response);
                return;
            }

            // place the blocks for this mechanic
            Buildings.build(sign.getWorld(), mechanic, Collections.singletonList(sign.getLocation()));
            mechanic.onBlocksLoaded(owner);

            Routes.removeNearbyRoutes(on);

            // add default members
            Bukkit.getScheduler().runTaskAsynchronously(Factorio.get(), () -> {
                try {
                    for (UUID defaultMember : Factorio.get().getMechanicController().getDefaultMembersFor(owner.getUniqueId())) {
                        mechanic.getManagement().getMembers().add(defaultMember);
                    }
                } catch (SQLException ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "A SQL error occurred!", ex);
                    owner.sendMessage("§cDer opstod en fejl ved tilføjelse af standard medlemmer. Kontakt en udvikler.");
                }
            });

            MechanicLoadEvent postEvent = new MechanicLoadEvent(mechanic);
            Bukkit.getPluginManager().callEvent(postEvent);

            sign.getWorld().playSound(sign.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.475f, 1f);

            callback.accept(MechanicBuildResponse.SUCCESS);
        });
    }

    public void loadMechanic(Sign sign, Consumer<Boolean> callback) {
        try {
            loadMechanic0(sign, callback);
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to load mechanic at " + sign.getBlock());
        }
    }

    private void loadMechanic0(Sign sign, Consumer<Boolean> callback) {
        Optional<MechanicProfile<?>> profile = getProfileFrom(sign);
        Block on = getBlockOn(sign);
        if (on != null && profile.isPresent()) {
            // check if there is already a mechanic at this location
            if (getMechanicAt(on.getLocation()) != null) {
                callback.accept(true);
                return;
            }

            BlockFace face = BlockUtil.getFacing(sign.getBlock());
            Query.CheckedSupplier<MechanicStorageContext, StorageException> contextSupplier = () -> contextProvider.findAt(on.getLocation());
            // load the mechanic
            loadMechanicFromSign(profile.get(), contextSupplier, sign, on, face, false, mechanic -> {
                // ensure only standing signs for buildings that allow it
                if (Tag.STANDING_SIGNS.isTagged(sign.getType()) && mechanic.getBuilding().deniesStandingSign()) {
                    callback.accept(false);
                    return;
                }

                mechanic.onBlocksLoaded(null);

                MechanicLoadEvent postEvent = new MechanicLoadEvent(mechanic);
                Bukkit.getPluginManager().callEvent(postEvent);

                callback.accept(true);
            });
        }
    }

    private void loadMechanicFromSign(MechanicProfile<?> profile, Query.CheckedSupplier<MechanicStorageContext, StorageException> contextSupplier, Sign sign, Block on, BlockFace rotation, boolean isBuild, Consumer<Mechanic<?>> callback) {
        // load this mechanic
        load(profile, contextSupplier, on.getLocation(), rotation, Tag.WALL_SIGNS.isTagged(sign.getType()), isBuild, mechanic -> {
            if (mechanic instanceof AccessibleMechanic) {
                sign.getSide(Side.FRONT).setLine(1, "Lvl " + mechanic.getLevel().lvl());
                sign.update();
            }

            callback.accept(mechanic);
        });
    }

    public void removeMechanic(Player player, Mechanic<?> mechanic) {
        // call mechanic remove event to event handlers
        MechanicRemoveEvent removeEvent = new MechanicRemoveEvent(player, mechanic);
        Bukkit.getPluginManager().callEvent(removeEvent);
        if (removeEvent.isCancelled()) {
            player.sendMessage("§cDer opstod en fejl! Kontakt en udvikler.");
            return;
        }

        Movement.removeMechanic(player, mechanic);

        deleteMechanic(mechanic, b -> {
            if (!b) {
                player.sendMessage("§cDer opstod en fejl! Kontakt en udvikler.");
                return;
            }

            // player stuff
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.6f);
            player.sendMessage("§eDu fjernede maskinen " + mechanic + " ved " + Types.LOCATION.convert(mechanic.getLocation()) + ".");
        });
    }

    public void deleteMechanic(Mechanic<?> mechanic) {
        deleteMechanic(mechanic, b -> {});
    }

    public void deleteMechanic(Mechanic<?> mechanic, Consumer<Boolean> callback) {
        // call mechanic delete event to event handlers
        MechanicDeleteEvent deleteEvent = new MechanicDeleteEvent(mechanic);
        Bukkit.getPluginManager().callEvent(deleteEvent);
        if (deleteEvent.isCancelled()) {
            callback.accept(false);
            return;
        }

        Movement.removeMechanic(null, mechanic);

        // unload and delete this mechanic
        unregister(mechanic);
        LOADING_THREAD.submit(() -> {
            try {
                if (!Factorio.get().getContextProvider().deleteAt(mechanic.getLocation()))  {
                    callback.accept(false);
                } else {
                    Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                        Buildings.remove(mechanic, mechanic.getLocation(), mechanic.getRotation(), true);
                        Routes.removeNearbyRoutes(mechanic.getLocation().getBlock());

                        callback.accept(true);
                    });
                }
            } catch (StorageException ex) {
                Factorio.get().getLogger().log(Level.SEVERE, "Failed to delete mechanic at location " + mechanic.getLocation(), ex);
                callback.accept(false);
            }
        });
    }
}
