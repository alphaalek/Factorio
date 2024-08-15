package dk.superawesome.factorio.listeners;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.impl.circuits.Cauldron;
import dk.superawesome.factorio.mechanics.routes.Routes;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;

public class CauldronLevelChangeListener implements Listener {

    @EventHandler
    public void onCauldronLevelChange(CauldronLevelChangeEvent event) {
        Mechanic<?> cauldronMechanic = Factorio.get().getMechanicManager(event.getBlock().getWorld()).getMechanicAt(event.getBlock().getLocation());
        if (cauldronMechanic instanceof Cauldron) {
            ((Cauldron) cauldronMechanic).onCauldronLevelChange(event);
        }
    }
}
