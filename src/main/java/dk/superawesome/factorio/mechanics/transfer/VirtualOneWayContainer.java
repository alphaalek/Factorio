package dk.superawesome.factorio.mechanics.transfer;

import dk.superawesome.factorio.gui.MechanicGui;
import dk.superawesome.factorio.gui.MechanicStorageGui;
import dk.superawesome.factorio.mechanics.Mechanic;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class VirtualOneWayContainer implements Inventory {

    private int maxStack = 64;

    private final List<Integer> slots;
    private final Mechanic<?> mechanic;
    private final MechanicStorageGui gui;

    public VirtualOneWayContainer(Mechanic<?> mechanic, MechanicStorageGui gui, List<Integer> slots) {
        this.mechanic = mechanic;
        this.gui = gui;
        this.slots = slots;
    }

    public MechanicStorageGui getStorageGui() {
        return gui;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public int getAmount() {
        return gui.calculateAmount(slots);
    }

    @Override
    public int getSize() {
        return slots.size();
    }

    @Override
    public int getMaxStackSize() {
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int i) {
        this.maxStack = i;
    }

    @Override
    public ItemStack getItem(int i) {
        return gui.getInventory().getItem(slots.get(i));
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        gui.getInventory().setItem(slots.get(i), itemStack);
    }

    @Override
    public HashMap<Integer, ItemStack> addItem(ItemStack... itemStacks) throws IllegalArgumentException {
        HashMap<Integer, ItemStack> remaining = new HashMap<>();
        int i = 0;
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null) {
                int left = gui.updateAddedItems(itemStack.getAmount(), itemStack, slots);
                if (left > 0) {
                    ItemStack newItem = itemStack.clone();
                    newItem.setAmount(left);
                    remaining.put(i, newItem);
                }
            }
            i++;
        }

        return remaining;
    }

    @Override
    public HashMap<Integer, ItemStack> removeItem(ItemStack... itemStacks) throws IllegalArgumentException {
        HashMap<Integer, ItemStack> remaining = new HashMap<>();
        int i = 0;
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null) {
                int left = gui.updateRemovedItems(itemStack.getAmount(), itemStack, MechanicGui.reverseSlots(slots));
                if (left > 0) {
                    ItemStack newItem = itemStack.clone();
                    newItem.setAmount(left);
                    remaining.put(i, newItem);
                }
            }
            i++;
        }

        return remaining;
    }

    @Override
    public ItemStack[] getContents() {
        return MechanicGui.reverseSlots(slots).stream().map(gui.getInventory()::getItem).toArray(ItemStack[]::new);
    }

    @Override
    public void setContents(ItemStack[] itemStacks) throws IllegalArgumentException {
        int i = 0;
        for (int slot : slots) {
            gui.getInventory().setItem(slot, itemStacks[i++]);
        }
    }

    @Override
    public ItemStack[] getStorageContents() {
        return getContents();
    }

    @Override
    public void setStorageContents(ItemStack[] itemStacks) throws IllegalArgumentException {
        setContents(itemStacks);
    }

    @Override
    public boolean contains(Material material) throws IllegalArgumentException {
        return Arrays.stream(getContents()).filter(Objects::nonNull).map(ItemStack::getType).anyMatch(material::equals);
    }

    @Override
    public boolean contains(ItemStack itemStack) {
        return Arrays.stream(getContents()).filter(Objects::nonNull).anyMatch(itemStack::isSimilar);
    }

    @Override
    public boolean contains(Material material, int i) throws IllegalArgumentException {
        return i >= Arrays.stream(getContents()).filter(Objects::nonNull).filter(item -> item.getType() == material).mapToInt(ItemStack::getAmount).sum();
    }

    @Override
    public boolean contains(ItemStack itemStack, int i) {
        return i >= Arrays.stream(getContents()).filter(Objects::nonNull).filter(itemStack::isSimilar).mapToInt(ItemStack::getAmount).sum();
    }

    @Override
    public boolean containsAtLeast(ItemStack itemStack, int i) {
        return contains(itemStack, i);
    }

    @Override
    public HashMap<Integer, ? extends ItemStack> all(Material material) throws IllegalArgumentException {
        HashMap<Integer, ItemStack> all = new HashMap<>();
        int i = 0;
        for (ItemStack item : getContents()) {
            if (item != null && item.getType() == material) {
                all.put(i, item);
            }
            i++;
        }

        return all;
    }

    @Override
    public HashMap<Integer, ? extends ItemStack> all(ItemStack itemStack) {
        HashMap<Integer, ItemStack> all = new HashMap<>();
        int i = 0;
        for (ItemStack item : getContents()) {
            if (item != null && item.isSimilar(itemStack)) {
                all.put(i, item);
            }
            i++;
        }

        return all;
    }

    @Override
    public int first(Material material) throws IllegalArgumentException {
        int[] s = {-1};
        Optional<Material> item = Arrays.stream(getContents()).peek(__ -> s[0]++).filter(Objects::nonNull).map(ItemStack::getType).filter(material::equals).findFirst();
        return item.isEmpty() ? -1 : s[0];
    }

    @Override
    public int first(ItemStack itemStack) {
        int[] s = {-1};
        Optional<ItemStack> item = Arrays.stream(getContents()).peek(__ -> s[0]++).filter(Objects::nonNull).filter(itemStack::isSimilar).findFirst();
        return item.isEmpty() ? -1 : s[0];
    }

    @Override
    public int firstEmpty() {
        return Arrays.stream(getContents()).noneMatch(Objects::isNull) ? -1
                : (int) Arrays.stream(getContents()).takeWhile(Objects::nonNull).count();
    }

    @Override
    public boolean isEmpty() {
        return Arrays.stream(getContents()).allMatch(Objects::isNull);
    }

    @Override
    public void remove(Material material) throws IllegalArgumentException {
        Arrays.stream(getContents()).filter(Objects::nonNull).filter(i -> i.getType() == material).forEach(i -> i.setAmount(0));
    }

    @Override
    public void remove(ItemStack itemStack) {
        Arrays.stream(getContents()).filter(Objects::nonNull).filter(itemStack::isSimilar).forEach(i -> i.setAmount(0));
    }

    @Override
    public void clear(int i) {
        setItem(i, null);
    }

    @Override
    public void clear() {
        Arrays.stream(getContents()).forEach(i -> i.setAmount(0));
    }

    @Override
    public List<HumanEntity> getViewers() {
        return new ArrayList<>(); // no viewers, this is a virtual inventory
    }

    @Override
    public InventoryType getType() {
        return InventoryType.CHEST;
    }

    @Override
    public InventoryHolder getHolder() {
        return null; // no holder, this is a virtual inventory
    }

    @Override
    public ListIterator<ItemStack> iterator() {
        return Arrays.stream(getContents()).collect(Collectors.toList()).listIterator();
    }

    @Override
    public ListIterator<ItemStack> iterator(int i) {
        return Arrays.stream(getContents()).collect(Collectors.toList()).listIterator(i);
    }

    @Override
    public Location getLocation() {
        return mechanic.getLocation();
    }

}