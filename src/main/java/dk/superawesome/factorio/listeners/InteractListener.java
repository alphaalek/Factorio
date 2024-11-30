package dk.superawesome.factorio.listeners;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.building.Matcher;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.util.TickValue;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
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
            if (event.isCancelled()) {
                return;
            }

            MechanicManager manager = Factorio.get().getMechanicManager(event.getClickedBlock().getWorld());
            Mechanic<?> mechanic = manager.getMechanicPartially(event.getClickedBlock().getLocation());
            if (mechanic != null) {

                if (event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.WOODEN_AXE)) {
                    event.setCancelled(true);
                    moveMechanic(event.getPlayer(), mechanic);
                } else if ((!event.getPlayer().isSneaking() || event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.AIR))
                        && !mechanic.getProfile().isInteractable() && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    if (mechanic.getProfile() instanceof GuiMechanicProfile<?>) {
                        interactMechanic(event.getPlayer(), mechanic);
                    }
                }
            } else if (event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.WOODEN_AXE)
                    && !checkDoubleAccess(event.getPlayer())) {
                event.setCancelled(true);

                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    Movement.finishMovement(event.getPlayer(), event.getClickedBlock().getRelative(BlockFace.UP).getLocation());
                } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    Movement.unregisterMovement(event.getPlayer());
                }
            }
        } else if (event.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.WOODEN_AXE) && event.getAction() == Action.LEFT_CLICK_AIR
                && !checkDoubleAccess(event.getPlayer())) {
            event.setCancelled(true);
            Movement.unregisterMovement(event.getPlayer());
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        MechanicManager manager = Factorio.get().getMechanicManager(event.getBlock().getWorld());
        Mechanic<?> mechanic = manager.getMechanicPartially(event.getBlock().getLocation());
        if (mechanic != null) {
            if (event.getPlayer().getInventory().getItemInMainHand().equals(Material.WOODEN_AXE)) {
                event.setCancelled(true);
                moveMechanic(event.getPlayer(), mechanic);
            } else if (mechanic.getBuilding() instanceof Matcher) {
                // check access
                if (!mechanic.getManagement().hasAccess(event.getPlayer(), Management.DELETE)) {
                    event.getPlayer().sendMessage("§cDu har ikke adgang til at fjerne maskinen (" + Bukkit.getOfflinePlayer(mechanic.getManagement().getOwner()).getName() + ")!");
                    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4f, 0.4f);
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

    private boolean checkDoubleAccess(Player player) {
        // ensure no double messages for blocks that calls interact event twice (e.g. interacting with Power Central)
        if (interactedPlayers.get().contains(player.getUniqueId())) {
            return true;
        }
        interactedPlayers.get().add(player.getUniqueId());
        return false;
    }

    private void moveMechanic(Player player, Mechanic<?> mechanic) {
        if (checkDoubleAccess(player)) {
            return;
        }

        // check if the player has access to move this mechanic
        if (!mechanic.getManagement().hasAccess(player, Management.MOVE)) {
            // no access
            player.sendMessage("§cDu har ikke adgang til at rykke denne maskine! (" + Bukkit.getOfflinePlayer(mechanic.getManagement().getOwner()).getName() + ")");
            player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4f, 0.4f);
            return;
        }

        Movement.registerMovement(player, mechanic);
    }

    private void interactMechanic(Player player, Mechanic<?> mechanic) {
        if (mechanic instanceof AccessibleMechanic accessible) {
            if (checkDoubleAccess(player)) {
                return;
            }

            // check if the player has access to open this mechanic
            if (!mechanic.getManagement().hasAccess(player, Management.OPEN)) {
                // no access
                player.sendMessage("§cDu har ikke adgang til at åbne denne maskine! (" + Bukkit.getOfflinePlayer(mechanic.getManagement().getOwner()).getName() + ")");
                player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.4f, 0.4f);
                return;
            }

            // open the mechanic inventory
            if (accessible.openInventory(mechanic, player)) {
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.25f, 0.5f);
            }
        }
    }
}
