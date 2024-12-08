package dk.superawesome.factorio.commands.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.commands.AbstractCommand;
import dk.superawesome.factorio.mechanics.Management;
import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Function;

public class AddMember extends AbstractCommand {

    protected static Mechanic<?> modifyMember(Function<String, Optional<OfflinePlayer>> getTarget, Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage("§cVælg en spiller!");
            return null;
        }
        Optional<OfflinePlayer> target = getTarget.apply(args[0]);
        if (target.isEmpty() || target.get().getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cKunne ikke finde spilleren " + args[0] + ".");
            return null;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            player.sendMessage("§cDu skal kigge på en maskine!");
            return null;
        }
        Mechanic<?> mechanic = Factorio.get().getMechanicManager(player.getWorld()).getMechanicAt(targetBlock.getLocation());
        if (mechanic == null) {
            player.sendMessage("§cDu skal kigge på en maskine!");
            return null;
        }

        if (!mechanic.getManagement().hasAccess(player, Management.MODIFY_MEMBERS)) {
            player.sendMessage("§cDu har ikke adgang til at ændre på medlemmerne af maskinen " + mechanic + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.5f);
            return null;
        }

        return mechanic;
    }

    @Override
    public void execute(Player player, String[] args) {
        Mechanic<?> mechanic = modifyMember(this::getTarget, player, args);
        if (mechanic != null) {
            OfflinePlayer target = getTarget(args[0]).get(); // should never be empty
            mechanic.getManagement().getMembers().add(target.getUniqueId());
            player.sendMessage("§eDu tilføjede spilleren " + target.getName() + " som medlem af maskinen " + mechanic + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.35f, 0.5f);
        }
    }
}
