package dk.superawesome.factorio;

import dk.superawesome.factorio.commands.Debug;
import dk.superawesome.factorio.commands.impl.*;
import dk.superawesome.factorio.listeners.*;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.db.DatabaseConnection;
import dk.superawesome.factorio.mechanics.db.MechanicController;
import dk.superawesome.factorio.util.Tick;
import org.bukkit.*;
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
    private MechanicController mechanicController;

    @Override
    @SuppressWarnings("ConstantConditions")
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
        this.mechanicController = controller;
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

        getCommand("addmembertoall").setExecutor(new AddMemberToAll());
        getCommand("listdefaultmembers").setExecutor(new ListDefaultMembers());
        getCommand("adddefaultmember").setExecutor(new AddDefaultMember());
        getCommand("removedefaultmember").setExecutor(new RemoveDefaultMember());
        getCommand("removememberfromall").setExecutor(new RemoveMemberFromAll());
        getCommand("debug").setExecutor(new Debug());

        Tick.start();

        Bukkit.getScheduler().runTask(this, () -> {
            // load all mechanics
            for (World world : Bukkit.getWorlds()) {
                MechanicManager manager = getMechanicManager(world);
                manager.loadMechanics();
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

    public MechanicController getMechanicController() {
        return mechanicController;
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
