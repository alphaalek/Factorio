package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.gui.BaseGui;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

public interface AccessibleMechanic {

    default <G extends BaseGui<G>, M extends Mechanic<M>> G getOrCreateInventory(Mechanic<?> mechanic) {
        G inUse = (G) mechanic.getGuiInUse().get();
        if (inUse != null) {
            return inUse;
        }

        if (mechanic.getProfile() instanceof GuiMechanicProfile<?>) {
            G gui = ((GuiMechanicProfile<M>) mechanic.getProfile()).<G>getGuiFactory().create((M) this, mechanic.getGuiInUse());
            mechanic.<G>getGuiInUse().set(gui);
            return gui;
        }

        return null;
    }

    default <G extends BaseGui<G>> boolean openInventory(Mechanic<?> mechanic, Player player) {
        G gui = getOrCreateInventory(mechanic);
        if (gui != null) {
            // Todo: remove this if we found a solution for a dupe bug when multiple players interact in the gui
            if (!gui.getInventory().getViewers().stream()
                    .map(HumanEntity::getName)
                    .allMatch(player.getName()::equals)) {
                player.sendMessage("§cDenne maskine er allerede åben af en anden spiller");
                return false;
            }

            // check if the player is already looking in this inventory
            if (gui.getInventory().getViewers().contains(player)) {
                return false;
            }

            player.openInventory(gui.getInventory());
            return true;
        }

        return false;
    }
}
