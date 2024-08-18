package dk.superawesome.factorio.commands.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.commands.AbstractCommand;
import dk.superawesome.factorio.mechanics.Management;
import dk.superawesome.factorio.mechanics.Mechanic;
import dk.superawesome.factorio.mechanics.MechanicManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class AddMemberToAll extends AbstractCommand {

    @Override
    public void execute(Player player, String[] args) {
        MechanicManager manager = Factorio.get().getMechanicManager(player.getWorld());
        List<Mechanic<?>> owned = manager.getAllMechanics().stream()
                .filter(m -> m.getManagement().hasAccess(player, Management.MODIFY_MEMBERS))
                .filter(m -> !player.isOp() || m.getManagement().getOwner().equals(player.getUniqueId()))
                .toList();
        if (owned.isEmpty()) {
            player.sendMessage("§cDu har ingen maskiner i verdenen " + player.getWorld().getName() + ".");
            return;
        }

        if (args.length == 0) {
            player.sendMessage("§cVælg en spiller!");
            return;
        }
        Optional<OfflinePlayer> target = getTarget(args[0]);
        if (target.isEmpty()) {
            player.sendMessage("§cKunne ikke finde spilleren " + args[0]);
            return;
        }

        for (Mechanic<?> mechanic : owned) {
            mechanic.getManagement().getMembers().add(target.get().getUniqueId());
        }
        player.sendMessage("§eDu har nu tilføjet spilleren " + target.get().getName() + " til alle dine maskiner. (" + owned.size()  + ")");
    }
}
