package dk.superawesome.factories.gui.impl;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.gui.BaseGui;
import dk.superawesome.factories.gui.MechanicGui;
import dk.superawesome.factories.mehcanics.impl.StorageBox;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StorageBoxGui extends MechanicGui<StorageBox> {

    private static final List<Integer> GRAY = Arrays.asList(45, 46, 47, 51, 53);
    private static final List<Integer> BLACK = IntStream.range(36, 45).boxed().collect(Collectors.toList());

    static {
        BLACK.addAll(Arrays.asList(48, 50));
    }

    public StorageBoxGui(StorageBox mechanic, AtomicReference<BaseGui> inUseReference) {
        super(mechanic, inUseReference, new InitCallbackHolder());
        initCallback.call();
    }

    @Override
    public void loadItems() {
        super.loadItems();

        for (int i : GRAY) {
            getInventory().setItem(i, BaseGui.GRAY);
        }
        for (int i : BLACK) {
            getInventory().setItem(i, BaseGui.BLACK);
        }
        getInventory().setItem(35, new ItemStack(Material.FEATHER));
        getInventory().setItem(49, new ItemStack(Material.MINECART));

        if (getMechanic().getStored() != null) {
            int a = getMechanic().getAmount();
            int i = 0;
            while (a > 0 && i < 35) {
                ItemStack item = getMechanic().getStored().clone();
                int amount = Math.min(item.getMaxStackSize(), a);

                item.setAmount(amount);
                a -= amount;

                getInventory().setItem(i++, item);
            }
        }
    }

    public List<ItemStack> findItems() {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 35; i++) {
            ItemStack item = getInventory().getItem(i);
            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    @Override
    public void onClose() {

    }

    private void updateAmount() {
        getMechanic().getTickThrottle().throttle();

        int before = findItems().stream()
                .mapToInt(ItemStack::getAmount).sum();
        Bukkit.getScheduler().runTask(Factories.get(), () -> {
            int after = findItems().stream()
                    .mapToInt(ItemStack::getAmount).sum();

            // get the difference in the items of the current inventory view of the storage box
            int diff = before - after;
            getMechanic().setAmount(getMechanic().getAmount() - diff);

            if (getMechanic().getAmount() == 0) {
                getMechanic().setStored(null);
            }
        });
    }

    private boolean handleInteract(ItemStack cursor) {
        // check if a player tries to add an item to the storage box which is not the one currently being stored
        if (cursor != null
                && cursor.getType() != Material.AIR
                && getMechanic().getStored() != null
                && !cursor.isSimilar(getMechanic().getStored())) {
            return true;
        }

        // update stored stack
        if (cursor != null
                && cursor.getType() != Material.AIR
                && getMechanic().getStored() == null) {
            ItemStack stored = cursor.clone();
            stored.setAmount(1);
            getMechanic().setStored(stored);
        }

        return false;
    }

    @Override
    public boolean onDrag(InventoryDragEvent event) {
        if (event.getInventorySlots().stream().anyMatch(i -> i < 35)) {
            if (handleInteract(event.getCursor())) {
                return true;
            }

            for (ItemStack item : event.getNewItems().values()) {
                if (handleInteract(item)) {
                    return true;
                }
            }

            if (getMechanic().getTickThrottle().isThrottled()) {
                return true;
            }

            updateAmount();
            return false;
        }

        return true;
    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        if (event.getSlot() < 35) {
            if (handleInteract(event.getCursor())) {
                return true;
            }

            return getMechanic().getTickThrottle().isThrottled();
        }

        return true;
    }

    @Override
    public boolean onClickOpen(InventoryClickEvent event) {
        if (movedFromOtherInventory(event)) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                    && event.getClickedInventory() != getInventory()
                    && handleInteract(event.getCurrentItem())) {
                return true;
            }

            if ((event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.HOTBAR_SWAP)
                    && (event.getCurrentItem() == null || findItems().size() > 1)
                    && handleInteract(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()))) {
                return true;
            }

            return getMechanic().getTickThrottle().isThrottled();
        }

        return false;
    }

    @Override
    public void onClickPost(InventoryClickEvent event) {
        if (!event.isCancelled()) {
            updateAmount();
        }
    }
}
