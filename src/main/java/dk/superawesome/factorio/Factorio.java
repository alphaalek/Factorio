package dk.superawesome.factorio;

import dk.superawesome.factorio.listeners.*;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.db.DatabaseConnection;
import dk.superawesome.factorio.mechanics.db.MechanicController;
import dk.superawesome.factorio.util.Tick;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class Factorio extends JavaPlugin implements Listener {

    private static Factorio instance;

    private final Map<String, MechanicManager> mechanicManagers = new HashMap<>();
    private final MechanicSerializer mechanicSerializer = new MechanicSerializer();
    private MechanicStorageContext.Provider contextProvider;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        ConfigurationSection sec = getConfig().getConfigurationSection("database");
        if (sec == null) {
            throw new RuntimeException("Could not find database configuration section");
        }
        DatabaseConnection connection = new DatabaseConnection(sec);
        Management.Serializer managementSerializer = new Management.Serializer(this.mechanicSerializer);
        MechanicController controller = new MechanicController(connection, this.mechanicSerializer, managementSerializer);
        this.contextProvider = new MechanicStorageContext.Provider(controller);

        for (World world : Bukkit.getServer().getWorlds()) {
            MechanicManager mm = new MechanicManager(world, contextProvider);
            mechanicManagers.put(world.getName(), mm);
        }

        Bukkit.getPluginManager().registerEvents(new ChunkLoadListener(), this);
        Bukkit.getPluginManager().registerEvents(new InteractListener(), this);
        Bukkit.getPluginManager().registerEvents(new PistonExtendListener(), this);
        Bukkit.getPluginManager().registerEvents(new SignChangeListener(), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(), this);
        Bukkit.getPluginManager().registerEvents(new ShopManager(), this);
        Bukkit.getPluginManager().registerEvents(new CauldronLevelChangeListener(), this);

        Tick.start();

        Bukkit.getScheduler().runTask(this, () -> {
            // load all mechanics
            for (World world : Bukkit.getWorlds()) {
                MechanicManager manager = getMechanicManager(world);
                for (Chunk chunk : world.getLoadedChunks()) {
                    for (BlockState state : chunk.getTileEntities()) {
                        if (state instanceof Sign && Tag.WALL_SIGNS.isTagged(state.getType())
                                && manager.getMechanicAt(BlockUtil.getPointingBlock(state.getBlock(), false).getLocation()) == null) {
                            // load this mechanic
                            manager.loadMechanic((Sign) state);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onDisable() {
        // save all mechanics
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                Factorio.get().getMechanicManager(world).unloadMechanics(chunk);
            }
        }
    }

    public static Factorio get() {
        return instance;
    }

    public MechanicStorageContext.Provider getContextProvider() {
        return contextProvider;
    }

    public <E extends Event> void registerEvent(Class<? extends E> clazz, EventPriority priority, Consumer<E> listener) {
        Bukkit.getPluginManager().registerEvent(clazz, this, priority, (l, event) -> listener.accept((E) event), this);
    }

    public MechanicManager getMechanicManager(World world) {
        return mechanicManagers.computeIfAbsent(world.getName(), d -> new MechanicManager(world, contextProvider));
    }

    public MechanicManager getMechanicManagerFor(Mechanic<?> mechanic) {
        return getMechanicManager(mechanic.getLocation().getWorld());
    }
}
