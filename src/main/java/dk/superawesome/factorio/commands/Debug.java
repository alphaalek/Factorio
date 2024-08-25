package dk.superawesome.factorio.commands;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class Debug extends AbstractCommand {

    @Override
    public void execute(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage("§cDu har ikke adgang til dette!");
            return;
        }

        Block block = player.getTargetBlockExact(5);
        if (block == null) {
            player.sendMessage("§cDu kigger ikke på en block!");
            return;
        }

        Mechanic<?> mechanic = Factorio.get().getMechanicManager(player.getWorld()).getMechanicPartially(block.getLocation());
        if (mechanic == null) {
            player.sendMessage("§cDu skal kigge på en maskine!");
            return;
        }

        player.sendMessage("§e" + mechanic.getProfile().getName() + " Lvl " + mechanic.getLevel());
        Class<?> clazz = mechanic.getClass();
        try {
            while (clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    player.sendMessage(field.getName() + ": §e" + field.get(mechanic));
                }

                clazz = clazz.getSuperclass();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
