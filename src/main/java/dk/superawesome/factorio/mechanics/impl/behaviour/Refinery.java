package dk.superawesome.factorio.mechanics.impl.behaviour;

import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.FluidCollection;
import dk.superawesome.factorio.mechanics.transfer.FluidContainer;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.BrewingStand;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class Refinery extends AbstractMechanic<Refinery> implements AccessibleMechanic, FluidContainer, ItemCollection {

    private final DelayHandler transferDelayHandler = new DelayHandler(10);

    private int bottleAmount;
    private ItemStack bottleType;

    private ItemStack storageType;
    private int storageAmount;

    public Refinery(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
        loadFromStorage();
    }

    @Override
    public void load(MechanicStorageContext context) throws Exception {
        ByteArrayInputStream str = context.getData();
        this.bottleAmount = context.getSerializer().readInt(str);
        this.bottleType = context.getSerializer().readItemStack(str);
        this.storageType = context.getSerializer().readItemStack(str);
        this.storageAmount = context.getSerializer().readInt(str);
    }

    @Override
    public void save(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        context.getSerializer().writeInt(str, this.bottleAmount);
        context.getSerializer().writeItemStack(str, this.bottleType);
        context.getSerializer().writeItemStack(str, this.storageType);
        context.getSerializer().writeInt(str, this.storageAmount);

        context.uploadData(str);
    }

    @Override
    public MechanicProfile<Refinery> getProfile() {
        return Profiles.REFINERY;
    }

    @Override
    public int getCapacity() {
        return level.getInt(ItemCollection.CAPACITY_MARK) *
            Optional.ofNullable(storageType)
                .map(ItemStack::getMaxStackSize)
                .orElse(64);
    }

    @Override
    public boolean has(ItemStack stack) {
        return has(i -> i.isSimilar(stack) && storageAmount >= stack.getAmount());
    }

    @Override
    public boolean has(Predicate<ItemStack> stack) {
        return storageType != null && stack.test(storageType);
    }

    @Override
    public List<ItemStack> take(int amount) {
        ItemStack item = storageType.clone();
        if (storageAmount > amount) {
            item.setAmount(amount);
        }

        storageAmount = Math.max(0, storageAmount - item.getAmount());
        if (storageAmount == 0) {
            storageType = null;
        }

        return List.of(item);
    }

    @Override
    public boolean isTransferEmpty() {
        return storageType == null;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return transferDelayHandler;
    }

    @Override
    public int getMaxTransfer() {
        return storageType.getMaxStackSize();
    }

    @Override
    public int getTransferAmount() {
        return storageAmount;
    }

    @Override
    public boolean isContainerEmpty() {
        return storageAmount == 0;
    }

    @Override
    public void pipePut(FluidCollection collection, PipePutEvent event) {

    }

    @Override
    public double getTransferEnergyCost() {
        return 1d;
    }

    public ItemStack getBottleType() {
        return bottleType;
    }

    public void setBottleType(ItemStack bottleType) {
        this.bottleType = bottleType;
    }

    public ItemStack getStorageType() {
        return storageType;
    }

    public void setStorageType(ItemStack stack) {
        this.storageType = stack;
    }

    public int getStorageAmount() {
        return storageAmount;
    }

    public void setStorageAmount(int amount) {
        this.storageAmount = amount;

        if (this.storageAmount == 0) {
            storageType = null;
        }
    }

    public int getBottleAmount() {
        return bottleAmount;
    }

    public void setBottleAmount(int amount) {
        this.bottleAmount = amount;
    }
}
