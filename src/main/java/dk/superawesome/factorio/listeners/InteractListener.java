package dk.superawesome.factorio.listeners;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.building.Matcher;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.util.TickValue;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InteractListener implements Listener {

    private final TickValue<List<UUID>> interactedPlayers = new TickValue<>(ArrayList::new);

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            MechanicManager manager = Factorio.get().getMechanicManager(event.getClickedBlock().getWorld());
            Mechanic<?> mechanic = manager.getMechanicPartially(event.getClickedBlock().getLocation());
            if (mechanic != null && !mechanic.getProfile().isInteractable() && event.getAction() == Action.RIGHT_CLICK_BLOCK && (!event.getPlayer().isSneaking() || event.getPlayer().getInventory().getItemInMainHand().getType() == Material.AIR)) {
                event.setCancelled(true);

                if (mechanic.getProfile() instanceof GuiMechanicProfile<?>) {
                    interactMechanic(event.getPlayer(), mechanic);
                }
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        MechanicManager manager = Factorio.get().getMechanicManager(event.getBlock().getWorld());
        Mechanic<?> mechanic = manager.getMechanicPartially(event.getBlock().getLocation());
        if (mechanic != null) {
            if (mechanic.getProfile().getBuilding() instanceof Matcher) {
                // check access
                if (!mechanic.getManagement().hasAccess(event.getPlayer(), Management.DELETE)) {
                    event.getPlayer().sendMessage("§cDu har ikke adgang til at fjerne maskinen!");
                    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                    event.setCancelled(true);
                    return;
                }

                Factorio.get().getMechanicManager(event.getBlock().getWorld()).removeMechanic(event.getPlayer(), mechanic);
            } else {
                event.setCancelled(true);
                interactMechanic(event.getPlayer(), mechanic);
            }
        }
    }

    private void interactMechanic(Player player, Mechanic<?> mechanic) {
        if (mechanic instanceof AccessibleMechanic accessible) {
            // ensure no double messages for blocks that calls interact event twice (e.g. interacting with Power Central)
            if (interactedPlayers.get().contains(player.getUniqueId())) {
                return;
            }
            interactedPlayers.get().add(player.getUniqueId());

            // check if the player has access to open this mechanic
            if (!mechanic.getManagement().hasAccess(player, Management.OPEN)) {
                // no access
                player.sendMessage("§cDu har ikke adgang til at åbne denne maskine!");
                player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 0.5f);
                return;
            }

            // open the mechanic inventory
            if (accessible.openInventory(mechanic, player)) {
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.375f, 0.5f);
            }
        }
    }
}
