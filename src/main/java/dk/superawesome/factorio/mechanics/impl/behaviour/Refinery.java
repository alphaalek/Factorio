package dk.superawesome.factorio.mechanics.impl.behaviour;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.impl.EmeraldForgeGui;
import dk.superawesome.factorio.gui.impl.RefineryGui;
import dk.superawesome.factorio.gui.impl.SmelterGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.BrewingStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Refinery extends AbstractMechanic<Refinery> implements AccessibleMechanic, FluidContainer, ItemCollection, Lightable {

    private final DelayHandler transferDelayHandler = new DelayHandler(10);
    private Block brewingStand;

    private int bottleAmount;

    private boolean declinedState;

    private Bottle bottleResult;
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
        ItemStack item = context.getSerializer().readItemStack(str);
        if (item != null) {
            this.bottleResult = Bottle.getBottleFromMaterial(item.getType()).orElseThrow(IllegalArgumentException::new);
        }
        this.storageType = context.getSerializer().readItemStack(str);
        this.storageAmount = context.getSerializer().readInt(str);
    }

    @Override
    public void save(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        context.getSerializer().writeInt(str, this.bottleAmount);
        context.getSerializer().writeItemStack(str,
            Optional.ofNullable(this.bottleResult)
                .map(Bottle::getOutputStack)
                .map(ItemStack::new)
                .orElse(null));

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

    // FluidContainer vv
    @Override
    public int getAmount() {
        return bottleAmount;
    }

    @Override
    public void setAmount(int amount) {
    }

    @Override
    public void addAmount(int amount) {
        bottleAmount += amount;
    }

    @Override
    public void removeAmount(int amount) {
        bottleAmount = Math.max(0, bottleAmount - amount);
    }

    @Override
    public void setFluidType(Material fluidType) {
        bottleResult = Bottle.getBottleFromMaterial(fluidType).orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public Material getFluidType() {
        return bottleResult.getLiquidStack().getType();
    }
    // FluidContainer ^^

    @Override
    public void onBlocksLoaded() {
        brewingStand = getLocation().getBlock().getRelative(0, 1, 0);
        if (brewingStand.getType() != Material.BREWING_STAND) {
            // invalid generator
            Factorio.get().getMechanicManager(getLocation().getWorld()).unload(this);
            Buildings.remove(loc.getWorld(), this);
            return;
        }

        // update block state
        updateLight();
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

    public Bottle getBottleResult() {
        return bottleResult;
    }

    public void setBottleResult(Bottle bottle) {
        this.bottleResult = bottle;
        this.<RefineryGui>getGuiInUse().get().loadLiquidSlots();
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

    public boolean isDeclined() {
        return declinedState;
    }

    public int getBottleAmount() {
        return bottleAmount;
    }

    public void setBottleAmount(int amount) {
        this.bottleAmount = amount;
    }

    @Override
    public void updateLight() {
        if (loc.getWorld() != null) {
            BlockData data = this.brewingStand.getBlockData();
            if (data instanceof BrewingStand brewingStandData) {
                double storage = (double) storageAmount / (double) getCapacity();

                // Reset all bottles
                brewingStandData.setBottle(0, false);
                brewingStandData.setBottle(1, false);
                brewingStandData.setBottle(2, false);

                if (storage > 0.66) {
                    brewingStandData.setBottle(2, true); // Turn on the third bottle
                }
                if (storage > 0.33) {
                    brewingStandData.setBottle(1, true); // Turn on the second bottle
                }
                if (storage > 0) {
                    brewingStandData.setBottle(0, true); // Turn on the first bottle
                }

                this.brewingStand.setBlockData(brewingStandData);
            }
        }
    }

    public enum Bottle {
        LAVA_BUCKET(Material.BUCKET, Material.LAVA, () -> new ItemStack(Material.LAVA_BUCKET), 3),
        WATER_BUCKET(Material.BUCKET, Material.WATER, () -> new ItemStack(Material.WATER_BUCKET), 3),

        WATER_BOTTLE(Material.GLASS_BOTTLE, Material.WATER, () -> {
            ItemStack bottle = new ItemStack(Material.POTION, 1);
            ItemMeta meta = bottle.getItemMeta();
            ((PotionMeta) meta).setBasePotionData(new PotionData(PotionType.WATER));
            bottle.setItemMeta(meta);
            return bottle;
        }, 1),
        ;

        private final Material bottleType;
        private final Material liquidType;
        private final ItemStack output;
        private final int capacity;
        Bottle(Material bottleType, Material liquidType, Supplier<ItemStack> output, int capacity) {
            this.bottleType = bottleType;
            this.liquidType = liquidType;
            this.output = output.get();
            this.capacity = capacity;
        }

        public static List<Bottle> getBottles() {
            return Arrays.asList(Bottle.values());
        }

        public static Optional<Bottle> getBottleFromLiquid(Material type) {
            return getBottles().stream().filter(b -> b.liquidType == type).findFirst();
        }

        public static Optional<Bottle> getBottleFromMaterial(Material type) {
            return getBottles().stream().filter(b -> b.output.getType() == type).findFirst();
        }

        public int getCapacity() {
            return capacity;
        }

        public ItemStack getBottleStack() {
            return new ItemStack(bottleType);
        }

        public ItemStack getLiquidStack() {
            return new ItemStack(liquidType);
        }

        public ItemStack getOutputStack() {
            return new ItemStack(output);
        }

        @Override
        public String toString() {
            return name().toLowerCase().replace("_", " ");
        }
    }
}
