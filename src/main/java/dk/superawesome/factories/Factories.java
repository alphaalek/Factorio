package dk.superawesome.factories;

import dk.superawesome.factories.listeners.*;
import dk.superawesome.factories.mechanics.Management;
import dk.superawesome.factories.mechanics.MechanicManager;
import dk.superawesome.factories.mechanics.MechanicSerializer;
import dk.superawesome.factories.mechanics.MechanicStorageContext;
import dk.superawesome.factories.mechanics.db.DatabaseConnection;
import dk.superawesome.factories.mechanics.db.MechanicController;
import dk.superawesome.factories.util.Tick;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class Factories extends JavaPlugin implements Listener {

    private static Factories instance;

    private final Map<World, MechanicManager> mechanicManagers = new HashMap<>();
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
            mechanicManagers.put(world, mm);
        }

        Bukkit.getPluginManager().registerEvents(new ChunkLoadListener(), this);
        Bukkit.getPluginManager().registerEvents(new InteractListener(), this);
        Bukkit.getPluginManager().registerEvents(new PistonExtendListener(), this);
        Bukkit.getPluginManager().registerEvents(new SignChangeListener(), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(), this);

        Tick.start();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Factories get() {
        return instance;
    }

    public <E extends Event> void registerEvent(Class<? extends E> clazz, EventPriority priority, Consumer<E> listener) {
        Bukkit.getPluginManager().registerEvent(clazz, this, priority, (l, event) -> listener.accept((E) event), this);
    }

    public MechanicManager getMechanicManager(World world) {
        return mechanicManagers.computeIfAbsent(world, d -> new MechanicManager(world, contextProvider));
    }
}
