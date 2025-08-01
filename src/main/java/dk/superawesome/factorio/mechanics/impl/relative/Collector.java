package dk.superawesome.factorio.mechanics.impl.relative;

import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
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

    public Collector(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public MechanicProfile<Collector> getProfile() {
        return Profiles.COLLECTOR;
    }

    public boolean handleInput(Material item) {
        Block above = getLocation().getBlock().getRelative(BlockFace.UP);
        this.collected = new ItemStack(item);
        Routes.startTransferRoute(above, this, this, true);

        // if the collected field is null, it means that the item was collected by some container
        boolean didCollect = this.collected == null;
        this.collected = null;
        return didCollect;
    }

    @Override
    public boolean has(ItemStack stack) {
        return has(i -> i.isSimilar(stack)) && this.collected.getAmount() >= stack.getAmount();
    }

    @Override
    public boolean has(Predicate<ItemStack> stack) {
        return this.collected != null && stack.test(this.collected);
    }

    @Override
    public List<ItemStack> pipeTake(int amount) {
        ItemStack item = this.collected.clone();
        if (this.collected.getAmount() > amount) {
            item.setAmount(amount);
        }

        this.collected.setAmount(Math.max(0, this.collected.getAmount() - item.getAmount()));
        if (this.collected.getAmount() == 0) {
            this.collected = null;
        }

        return Collections.singletonList(item);
    }

    @Override
    public boolean isTransferEmpty() {
        return this.collected == null;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return DelayHandler.NO_DELAY;
    }

    @Override
    public double getMaxTransfer() {
        return this.collected.getMaxStackSize();
    }

    @Override
    public double getTransferAmount() {
        return this.collected.getAmount();
    }

    @Override
    public double getTransferEnergyCost() {
        return -1;
    }
}
