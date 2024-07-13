package dk.superawesome.factories;

import dk.superawesome.factories.listeners.ChunkLoadListener;
import dk.superawesome.factories.listeners.RedstoneSignalListener;
import dk.superawesome.factories.mehcanics.MechanicManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Factories extends JavaPlugin {

    private static Factories instance;

    private MechanicManager mechanigManager;

    @Override
    public void onEnable() {
        instance = this;
        this.mechanigManager = new MechanicManager();

        Bukkit.getPluginManager().registerEvents(new RedstoneSignalListener(), this);
        Bukkit.getPluginManager().registerEvents(new ChunkLoadListener(), this);
        Bukkit.getPluginManager().registerEvents(this.mechanigManager, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Factories get() {
        return instance;
    }

    public MechanicManager getMechanicManager() {
        return mechanigManager;
    }
}
