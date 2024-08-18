package dk.superawesome.factorio.commands.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.commands.AbstractCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class ListDefaultMembers extends AbstractCommand {

    @Override
    public void execute(Player player, String[] args) {
        try {
            List<UUID> members = Factorio.get().getMechanicController().getDefaultMembersFor(player.getUniqueId());
            if (members.isEmpty()) {
                player.sendMessage("§cDu har ingen standard medlemmer for dine nybyggede maskiner.");
                return;
            }

            player.sendMessage("§eDu har følgende standard medlemmer for dine nybyggede maskiner (" + members.size() + "):");
            for (UUID member : members) {
                player.sendMessage(" §e" + Bukkit.getOfflinePlayer(member).getName() + " (" + member + ")");
            }
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "A SQL error occurred!", ex);
            player.sendMessage("§cDer skete en fejl! Kontakt en udvikler.");
        }
    }
}
