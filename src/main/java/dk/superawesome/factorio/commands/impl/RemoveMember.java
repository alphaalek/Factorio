package dk.superawesome.factorio.commands.impl;

import dk.superawesome.factorio.commands.AbstractCommand;
import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class RemoveMember extends AbstractCommand {

    @Override
    public void execute(Player player, String[] args) {
        Mechanic<?> mechanic = AddMember.modifyMember(this::getTarget, player, args);
        if (mechanic != null) {
            OfflinePlayer target = getTarget(args[0]).get(); // should never be empty
            mechanic.getManagement().getMembers().remove(target.getUniqueId());
            player.sendMessage("§eDu fjernede spilleren " + target.getName() + " som medlem af maskinen " + mechanic + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.35f, 0.5f);
        }
    }
}
