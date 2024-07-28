package dk.superawesome.factorio.listeners;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.MechanicManager;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public class SignChangeListener implements Listener {

    @EventHandler
    public void onSignUpdate(SignChangeEvent event) {
        MechanicManager manager = Factorio.get().getMechanicManager(event.getBlock().getWorld());

        if (manager.getMechanicPartially(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        } else if (Tag.WALL_SIGNS.isTagged(event.getBlock().getType())) {
            Bukkit.getScheduler().runTask(Factorio.get(), () -> manager.buildMechanic((Sign) event.getBlock().getState(), event.getPlayer().getUniqueId()));
        }
    }
}
