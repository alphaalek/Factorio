package dk.superawesome.factories;

import dk.superawesome.factories.listeners.ChunkLoadListener;
import dk.superawesome.factories.listeners.RedstoneSignalListener;
import dk.superawesome.factories.production.ProductionHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Factories extends JavaPlugin {

    private static Factories instance;

    private ProductionHandler productionHandler;

    @Override
    public void onEnable() {
        instance = this;
        this.productionHandler = new ProductionHandler();

        Bukkit.getPluginManager().registerEvents(new RedstoneSignalListener(), this);
        Bukkit.getPluginManager().registerEvents(new ChunkLoadListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Factories get() {
        return instance;
    }

    public ProductionHandler getProductionHandler() {
        return productionHandler;
    }
}
