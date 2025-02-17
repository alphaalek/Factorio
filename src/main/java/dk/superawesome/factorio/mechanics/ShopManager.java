package dk.superawesome.factorio.mechanics;

import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.gui.MechanicStorageGui;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import dk.superawesome.factorio.mechanics.transfer.VirtualOneWayContainer;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class ShopManager implements Listener {

    private int recentAmount = -1;
    private VirtualOneWayContainer recentContainer;
    private ItemStack recentOffer;
    private int recentContext;

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPreTransaction(PreTransactionEvent event) {
        Mechanic<?> mechanic = Factorio.get().getMechanicManager(event.getSign().getWorld())
                .getMechanicAt(BlockUtil.getPointingBlock(event.getSign().getBlock(), true).getLocation());

        // server-sync call
        recentContainer = null;
        recentOffer = null;
        recentContext = -1;
        recentAmount = -1;
        if (mechanic instanceof ItemContainer container
                && mechanic instanceof ItemCollection collection
                && mechanic instanceof AccessibleMechanic accessible) {

            BaseGui<?> gui = accessible.getOrCreateInventory(mechanic);
            if (gui instanceof MechanicStorageGui storageGui) {
                TransactionEvent.TransactionType type = event.getTransactionType();
                boolean isInput = checkInput(storageGui, type);

                recentOffer = Arrays.stream(event.getStock())
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(new ItemStack(Material.AIR));

                // check if the related item can be sold/bought
                ItemStack stored = mechanic.getProfile().getStorageProvider().createStorage(mechanic, recentContext).getStored();
                if (stored != null && !stored.isSimilar(recentOffer)) {
                    event.getClient().sendMessage("§cDenne maskine har ikke det indhold som shop-skiltet viser!");
                    event.setCancelled(true);
                    return;
                }

                if (mechanic.getTickThrottle().tryThrottle()) {
                    event.setCancelled(true);
                    return;
                }

                event.setOwnerInventory(recentContainer = (VirtualOneWayContainer) container.createVirtualOneWayInventory(mechanic, collection, container, storageGui, isInput));
            }
            gui.handleClose(event.getClient());
        }
    }

    private boolean checkInput(MechanicStorageGui storageGui, TransactionEvent.TransactionType type) {
        switch (type) {
            case SELL -> {
                recentContext = storageGui.getInputContext();
                return true;
            }
            case BUY -> {
                recentContext = storageGui.getOutputContext();
                return false;
            }
        }

        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTransaction(TransactionEvent event) {
        if (recentContainer != null) {
            recentAmount = recentContainer.getAmount();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTransactionPost(TransactionEvent event) {
        if (recentAmount != -1 && recentContext != -1) {
            int diff = recentContainer.getAmount() - recentAmount;

            // update stored type and amount
            Storage storage = recentContainer.getStorageGui().getStorage(recentContext);
            if (storage.getStored() == null) {
                ItemStack stack = recentOffer.clone();
                stack.setAmount(1);
                storage.setStored(stack);
            }
            recentContainer.getStorageGui().updateAmount(recentContext, recentOffer, event.getClientInventory(), diff, recentContainer.getSlots());

            // if we were not able to add anything, clear the stored
            if (storage.getAmount() == 0) {
                storage.setStored(null);
            }
        }
    }
}
