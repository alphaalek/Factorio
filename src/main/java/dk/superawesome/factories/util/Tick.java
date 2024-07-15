package dk.superawesome.factories.util;

import dk.superawesome.factories.Factories;
import org.bukkit.Bukkit;

public class Tick {

    private static int currentTick;

    public static int get() {
        return currentTick;
    }

    public static void start() {
        Bukkit.getScheduler().runTaskTimer(Factories.get(), () -> {
            Tick.currentTick++;
        }, 0L, 1L);
    }
}
