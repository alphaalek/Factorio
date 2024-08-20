package dk.superawesome.factorio.commands.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.commands.AbstractCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;

public class RemoveDefaultMember extends AbstractCommand {

    @Override
    public void execute(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage("§cVælg en spiller!");
            return;
        }
        Optional<OfflinePlayer> target = getTarget(args[0]);
        if (target.isEmpty() || target.get().getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cKunne ikke finde spilleren " + args[0] + ".");
            return;
        }

        try {
            if (!Factorio.get().getMechanicController().getDefaultMembersFor(player.getUniqueId()).contains(target.get().getUniqueId())) {
                player.sendMessage("§cSpilleren " + target.get().getName() + " er ikke iblandt dine standard medlemmer.");
                return;
            }

            Factorio.get().getMechanicController().removeDefaultMemberFor(player.getUniqueId(), target.get().getUniqueId());
            player.sendMessage("§eDu fjernede " + target.get().getName() + " som standard medlem for dine nybyggede maskiner.");
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "A SQL error occurred!", ex);
            player.sendMessage("§cDer skete en fejl! Kontakt en udvikler.");
        }
    }
}
