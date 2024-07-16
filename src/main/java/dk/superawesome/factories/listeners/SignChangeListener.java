package dk.superawesome.factories.listeners;

import dk.superawesome.factories.Factories;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public class SignChangeListener implements Listener {

    @EventHandler
    public void onSignUpdate(SignChangeEvent event) {
        if (Factories.get().getMechanicManager(event.getBlock().getWorld())
                .getMechanicPartially(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }
}
