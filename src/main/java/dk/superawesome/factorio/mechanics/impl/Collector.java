package dk.superawesome.factorio.mechanics.impl;

import dk.superawesome.factorio.mechanics.AbstractMechanic;
import dk.superawesome.factorio.mechanics.MechanicProfile;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.mechanics.Profiles;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class Collector extends AbstractMechanic<Collector> implements ItemCollection {

    private ItemStack collected;

    public Collector(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public MechanicProfile<Collector> getProfile() {
        return Profiles.COLLECTOR;
    }

    public boolean handleInput(Material item) {
        Block above = getLocation().getBlock().getRelative(BlockFace.UP);
        collected = new ItemStack(item);
        Routes.startTransferRoute(above, this);

        // if the collected field is null, it means that the item was collected by some container
        boolean didCollect = collected == null;
        collected = null;
        return didCollect;
    }

    @Override
    public boolean has(ItemStack stack) {
        return has(i -> i.isSimilar(stack)) && collected.getAmount() >= stack.getAmount();
    }

    @Override
    public boolean has(Predicate<ItemStack> stack) {
        return collected != null && stack.test(collected);
    }

    @Override
    public List<ItemStack> take(int amount) {
        ItemStack item = collected.clone();
        if (collected.getAmount() > amount) {
            item.setAmount(amount);
        }

        collected.setAmount(Math.max(0, collected.getAmount() - item.getAmount()));
        if (collected.getAmount() == 0) {
            collected = null;
        }

        return Collections.singletonList(item);
    }

    @Override
    public boolean isTransferEmpty() {
        return collected != null;
    }

    @Override
    public double getTransferEnergyCost() {
        return 0;
    }
}
