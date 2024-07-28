package dk.superawesome.factorio.util;

import dk.superawesome.factorio.Factorio;
import org.bukkit.Bukkit;

public class Tick {

    private static int currentTick;

    public static int get() {
        return currentTick;
    }

    public static void start() {
        Bukkit.getScheduler().runTaskTimer(Factorio.get(), () -> {
            Tick.currentTick++;
        }, 0L, 1L);
    }
}
