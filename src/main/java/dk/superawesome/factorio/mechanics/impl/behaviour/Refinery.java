package dk.superawesome.factorio.mechanics.impl.behaviour;

import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.gui.impl.RefineryGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.stackregistry.Filled;
import dk.superawesome.factorio.mechanics.stackregistry.FluidStack;
import dk.superawesome.factorio.mechanics.stackregistry.Volume;
import dk.superawesome.factorio.mechanics.transfer.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class Refinery extends AbstractMechanic<Refinery> implements AccessibleMechanic, Container<TransferCollection>, ItemCollection {

    public static final int VOLUME_MARK = 1;

    private final DelayHandler transferDelayHandler = new DelayHandler(10);

    private int volumeAmount;
    private Volume volume;

    private int filledAmount;
    private Filled filled;

    private final ItemContainer delegatedItemContainer = new ItemContainer() {

        @Override
        public boolean isContainerEmpty() {
            return volume == null;
        }

        @Override
        public void pipePut(ItemCollection collection, PipePutEvent event) {
            ItemStack item = Optional.ofNullable(volume)
                    .map(Volume::getMat)
                    .map(ItemStack::new)
                    .orElse(null);
            if ((item == null || collection.has(item)) && volumeAmount < getVolumeCapacity()) {
                //TODO: remember to check for filled items and it's different output
//                Filled filled1 = Filled.getFilledState(volume, filled.getFluid()).orElse(null);
//                if (filledAmount > 0 && filled1 != null && !filled1.getOutputItemStack().isSimilar(filled.getOutputItemStack())) {
//                    return;
//                }
                int add = this.<RefineryGui>put(collection, getVolumeCapacity() - volumeAmount, getGuiInUse(), RefineryGui::updateAddedVolume, new HeapToStackAccess<ItemStack>() {
                    @Override
                    public ItemStack get() {
                        if (volume == null || volumeAmount == 0) {
                            return null;
                        } else {
                            return new ItemStack(volume.getMat());
                        }
                    }

                    @Override
                    public void set(ItemStack stack) {
                        volume = Volume.getType(stack.getType()).orElse(null);
                    }
                });

                if (add > 0) {
                    volumeAmount += add;
                    event.setTransferred(true);
                }
            }
        }

        @Override
        public int getCapacity() {
            return getVolumeCapacity();
        }
    };

    private final FluidContainer delegatedFluidContainer = new FluidContainer() {

        @Override
        public boolean isContainerEmpty() {
            return true; // always empty, because no fluid will be stored inside the mechanic
        }

        @Override
        public void pipePut(FluidCollection collection, PipePutEvent event) {
            // no empty bottle or not enough fluid to fill a bottle
            if (volumeAmount == 0 || volume == null || volume.getFluidRequires() > collection.getTransferAmount()) {
                return;
            }

            if ((filled == null || collection.hasFluid(filled.getFluid())) && filledAmount < getCapacity()) {
                // is an allowed combination
                if (Filled.getFilledState(volume, collection.getFluid()).isEmpty()) {
                    return;
                }
                event.setTransferred(true);
                int putAmount = Refinery.this.<RefineryGui>putFluid(collection, getCapacity() - filledAmount, getGuiInUse(), RefineryGui::updateAddedFilled, new HeapToStackAccess<>() {
                    @Override
                    public ItemStack get() {
                        return filled == null ? null : filled.getOutputItemStack();
                    }

                    @Override
                    public void set(ItemStack stack) {
                        filled = Filled.getFilledStateByStack(stack).orElse(null);
                    }
                });
                setFilledAmount(getFilledAmount() + putAmount);
                setBottleAmount(getBottleAmount() - putAmount);
            }
        }

        @Override
        public int getCapacity() {
            return -1; // no capacity needed, because no fluid will be stored inside the mechanic
        }
    };

    public Refinery(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
        loadFromStorage();
    }

    @Override
    public void load(MechanicStorageContext context) throws Exception {
        ByteArrayInputStream str = context.getData();

        this.volumeAmount = context.getSerializer().readInt(str);
        ItemStack volume = context.getSerializer().readItemStack(str);
        if (volume != null) {
            this.volume = Volume.getType(volume.getType()).orElse(null);
        }

        this.filledAmount = context.getSerializer().readInt(str);
        ItemStack filledStack = context.getSerializer().readItemStack(str);
        if (filledStack != null) {
            this.filled = Filled.getFilledStateByStack(filledStack).orElse(null);
            if (this.filled == null) {
                // invalid filled state
                this.filledAmount = 0;
            }
        }
    }

    @Override
    public void save(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayOutputStream str = new ByteArrayOutputStream();

        context.getSerializer().writeInt(str, this.volumeAmount);
        if (this.volume != null) {
            context.getSerializer().writeItemStack(str, new ItemStack(this.volume.getMat()));
        } else {
            context.getSerializer().writeItemStack(str, null);
        }

        context.getSerializer().writeInt(str, this.filledAmount);
        if (this.filled != null) {
            context.getSerializer().writeItemStack(str, new ItemStack(this.filled.getOutputItemStack()));
        } else {
            context.getSerializer().writeItemStack(str, null);
        }

        context.uploadData(str);
    }

    @Override
    public MechanicProfile<Refinery> getProfile() {
        return Profiles.REFINERY;
    }

    public int getVolumeCapacity() {
        return level.getInt(VOLUME_MARK) *
                Optional.ofNullable(volume)
                        .map(Volume::getMat)
                        .map(Material::getMaxStackSize)
                        .orElse(64);
    }

    @Override
    public int getCapacity() {
        return level.getInt(ItemCollection.CAPACITY_MARK) *
                Optional.ofNullable(filled)
                        .map(Filled::getVolume)
                        .map(Volume::getMat)
                        .map(Material::getMaxStackSize)
                        .orElse(64);
    }

    @Override
    public void pipePut(TransferCollection collection, PipePutEvent event) {
        if (tickThrottle.isThrottled()) {
            return;
        }

        if (collection instanceof ItemCollection itemCollection) {
            delegatedItemContainer.pipePut(itemCollection, event);
        } else if (collection instanceof FluidCollection fluidCollection) {
            delegatedFluidContainer.pipePut(fluidCollection, event);
        }
    }

    private <G extends BaseGui<G>> int putFluid(FluidCollection from, int take, AtomicReference<G> inUse, BiConsumer<G, Integer> doGui, HeapToStackAccess<ItemStack> access) {
        FluidStack fluidStack = from.take(take);
        int add = 0;

        if (access.get() == null) {
            ItemStack stack = Filled.getFilledState(volume, fluidStack.getFluid()).map(Filled::getOutputItemStack).orElse(null);
            access.set(stack);
        }

        while (!fluidStack.isEmpty()) {
            if (fluidStack.getAmount() >= volume.getFluidRequires()) {
                add += 1;
                fluidStack.setAmount(fluidStack.getAmount() - volume.getFluidRequires());
            }
        }

        if (add > 0) {
            G gui = inUse.get();
            if (gui != null) {
                doGui.accept(gui, add);
            }
        }

        return add;
    }

    @Override
    public boolean isTransferEmpty() {
        return filledAmount == 0;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return transferDelayHandler;
    }

    @Override
    public int getMaxTransfer() {
        return filled.getVolume().getMat().getMaxStackSize();
    }

    @Override
    public int getTransferAmount() {
        return filledAmount;
    }

    @Override
    public boolean accepts(TransferCollection collection) {
        return collection instanceof ItemCollection || collection instanceof FluidCollection;
    }

    @Override
    public boolean isContainerEmpty() {
        return delegatedFluidContainer.isContainerEmpty() && delegatedItemContainer.isContainerEmpty();
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d;
    }

    @Override
    public boolean has(ItemStack stack) {
        return has(i -> i.isSimilar(stack) && filledAmount >= stack.getAmount());
    }

    @Override
    public boolean has(Predicate<ItemStack> stack) {
        return filled != null && stack.test(filled.getOutputItemStack());
    }

    @Override
    public List<ItemStack> take(int amount) {
        return this.<RefineryGui>take(Math.min(getMaxTransfer(), amount), filled.getOutputItemStack(), filledAmount, getGuiInUse(), RefineryGui::updateRemovedFilled, new HeapToStackAccess<>() {
            @Override
            public Integer get() {
                return filledAmount;
            }

            @Override
            public void set(Integer val) {
                setFilledAmount(filledAmount - val);
            }
        });
    }

    public Volume getVolume() {
        return volume;
    }

    public void setVolume(Volume volume) {
        this.volume = volume;

        if (this.volume == null) {
            this.volumeAmount = 0;
        }
    }

    public Filled getFilled() {
        return filled;
    }

    public void setFilled(Filled filled) {
        this.filled = filled;

        if (this.filled == null) {
            filledAmount = 0;
        }
    }

    public int getFilledAmount() {
        return filledAmount;
    }

    public void setFilledAmount(int amount) {
        this.filledAmount = amount;

        if (this.filledAmount == 0) {
            filled = null;
        }
    }

    public int getBottleAmount() {
        return volumeAmount;
    }

    public void setBottleAmount(int amount) {
        this.volumeAmount = amount;

        if (this.volumeAmount == 0) {
            volume = null;
        }
    }
}
