package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.util.db.Types;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class Movement {

    private static final Map<UUID, Mechanic<?>> selections = new HashMap<>();

    public static void registerMovement(Player player, Mechanic<?> mechanic) {
        if (selections.containsKey(player.getUniqueId()) && selections.get(player.getUniqueId()) == mechanic) {
            return;
        }

        selections.put(player.getUniqueId(), mechanic);
        player.playSound(player.getLocation(), Sound.BLOCK_STONE_PLACE, 0.3f, 0.5f);
        player.sendMessage("§eDu er nu i gang med at flytte maskinen " + mechanic + " ved " + Types.LOCATION.convert(mechanic.getLocation()) + ".");
        player.sendMessage("§eVenstreklik på den block du ønsker at flytte maskinen over på. Højreklik for at annullere.");
    }

    public static void unregisterMovement(Player player) {
        if (selections.containsKey(player.getUniqueId())) {
            player.sendMessage("§cDu har nu annulleret flytningen af maskinen " + selections.get(player.getUniqueId()) + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.2f, 0.5f);
            selections.remove(player.getUniqueId());
        }
    }

    public static void finishMovement(Player player, Location to) {
        if (!selections.containsKey(player.getUniqueId())) {
            return;
        }
        Mechanic<?> mechanic = selections.get(player.getUniqueId());

        BlockFace rot = BlockUtil.getPrimaryDirection(player.getLocation().getYaw()).getOppositeFace();
        Block sign = mechanic.getLocation().getBlock().getRelative(mechanic.getRotation());
        if (Factorio.get().getMechanicManager(to.getWorld()).moveMechanic(player, mechanic, to, rot, sign)) {
            selections.remove(player.getUniqueId());
        }
    }

    public static void removeMechanic(Player by, Mechanic<?> mechanic) {
        Iterator<Map.Entry<UUID, Mechanic<?>>> iterator = selections.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Mechanic<?>> e = iterator.next();
            if (e.getValue() == mechanic) {
                iterator.remove();

                Player selectionPlayer = Bukkit.getPlayer(e.getKey());
                if (selectionPlayer != null && selectionPlayer != by) {
                    selectionPlayer.sendMessage("§cMaskinen du var i gang med at flytte er blevet fjernet af " + by.getName() + "!");
                }
                break;
            }
        }
    }
}
