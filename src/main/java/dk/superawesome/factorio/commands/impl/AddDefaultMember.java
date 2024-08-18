package dk.superawesome.factorio.commands.impl;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.commands.AbstractCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;

public class AddDefaultMember extends AbstractCommand {

    @Override
    public void execute(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage("§cVælg en spiller!");
            return;
        }
        Optional<OfflinePlayer> target = getTarget(args[0]);
        if (target.isEmpty()) {
            player.sendMessage("§cKunne ikke finde spilleren " + args[0]);
            return;
        }

        try {
            Factorio.get().getMechanicController().addDefaultMemberFor(player.getUniqueId(), target.get().getUniqueId());
            player.sendMessage("§eDu tilføjede " + target.get().getName() + " som standard medlem for dine nybyggede maskiner.");
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "A SQL error occurred!", ex);
            player.sendMessage("§cDer skete en fejl! Kontakt en udvikler.");
        }
    }
}
