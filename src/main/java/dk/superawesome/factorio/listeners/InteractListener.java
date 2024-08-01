package dk.superawesome.factorio.listeners;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.Management;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.MechanicManager;
import dk.superawesome.factorio.util.TickValue;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InteractListener implements Listener {

    private final TickValue<List<UUID>> interactedPlayers = new TickValue<>(ArrayList::new);

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked != null) {
            // check if the player clicked on a mechanic
            MechanicManager manager = Factorio.get().getMechanicManager(clicked.getWorld());
            Mechanic<?, ?> mechanic = manager.getMechanicPartially(clicked.getLocation());
            if (mechanic != null) {
                event.setCancelled(true);

                // check if the player has access to open this mechanic
                if (!mechanic.getManagement().hasAccess(event.getPlayer().getUniqueId(), Management.OPEN)) {
                    // ensure no double messages for blocks that calls interact event twice (e.g. interacting with Power Central)
                    if (interactedPlayers.get().contains(event.getPlayer().getUniqueId())) {
                        return;
                    }
                    interactedPlayers.get().add(event.getPlayer().getUniqueId());

                    // no access
                    event.getPlayer().sendMessage("§cDu har ikke adgang til at åbne denne maskine!");
                    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 0.5f);
                    return;
                }

                // open the mechanic inventory
                event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_CHEST_OPEN, 0.375f, 0.5f);
                mechanic.openInventory(event.getPlayer());
            }
        }
    }
}
