package dk.superawesome.factorio.listeners;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Matcher;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.util.db.Types;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

public class SignChangeListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignUpdate(SignChangeEvent event) {
        MechanicManager manager = Factorio.get().getMechanicManager(event.getBlock().getWorld());

        Mechanic<?> partiallyAt = manager.getMechanicPartially(event.getBlock().getLocation());
        if (partiallyAt != null) {
            // check access
            if (!partiallyAt.getManagement().hasAccess(event.getPlayer(), Management.MODIFY_SIGN)) {
                event.getPlayer().sendMessage("§cDu har ikke adgang til at ændre på dette skilt!");
                event.setCancelled(true);
                return;
            }

            // ensure first line persists to be identifiable for the mechanic at this sign block
            event.setLine(0, "[" + partiallyAt.getProfile().getSignName() + "]");
            if (partiallyAt instanceof AccessibleMechanic) {
                event.setLine(1, "Lvl " + partiallyAt.getLevel().lvl());
            }
            Bukkit.getScheduler().runTask(Factorio.get(), partiallyAt::onUpdate);
        } else if (manager.getProfileFrom(event).isPresent() &&
                (Tag.STANDING_SIGNS.isTagged(event.getBlock().getType()) || Tag.WALL_SIGNS.isTagged(event.getBlock().getType()))) {
            Block on = manager.getBlockOn((Sign) event.getBlock().getState());
            if (on == null) {
                event.getPlayer().sendMessage("§cDu kan ikke placere en maskine på denne block!");
                event.setCancelled(true);
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(event.getPlayer().getInventory().getItemInMainHand().getType()));
                event.getBlock().setType(Material.AIR);
                return;
            }

            // fix direction for standing signs
            if (Tag.STANDING_SIGNS.isTagged(event.getBlock().getType())) {
                BlockUtil.rotate(event.getBlock(), BlockUtil.getCartesianRotation(BlockUtil.getFacing(event.getBlock())));
            }

            // try to build the mechanic in the next tick
            Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                MechanicBuildResponse response = manager.buildMechanic((Sign) event.getBlock().getState(), on, event.getPlayer());
                build: {
                    switch (response) {
                        case SUCCESS -> {
                            // Success!
                            Mechanic<?> mechanic = manager.getMechanicPartially(event.getBlock().getLocation());
                            if (mechanic != null) {
                                event.getPlayer().sendMessage("§eDu oprettede maskinen " + mechanic.getProfile().getName() + " ved " + Types.LOCATION.convert(event.getBlock().getLocation()) + ".");
                            } else{
                                event.getBlock().setType(Material.AIR);
                            }
                            break build;
                        }
                        case NO_SUCH -> {
                            break build;
                        }

                        case ALREADY_EXISTS -> event.getPlayer().sendMessage("§cDer er allerede en maskine her.");
                        case ERROR -> event.getPlayer().sendMessage("§cDer skete en fejl under oprettelse af maskinen.");
                        case NOT_ENOUGH_SPACE -> event.getPlayer().sendMessage("§cDer er ikke nok plads til at bygge maskinen.");
                        case NOT_PLACED_BLOCKS -> event.getPlayer().sendMessage("§cMaskinen er ikke blevet bygget rigtigt.");
                    }

                    event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                    event.getBlock().setType(Material.AIR);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(event.getPlayer().getInventory().getItemInMainHand().getType()));
                }
            });
        }
    }
}
