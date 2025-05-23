package dk.superawesome.factorio.mechanics.impl.accessible;

import dk.superawesome.factorio.gui.impl.RefineryGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.stackregistry.Filled;
import dk.superawesome.factorio.mechanics.stackregistry.Volume;
import dk.superawesome.factorio.mechanics.transfer.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class Refinery extends AbstractMechanic<Refinery> implements AccessibleMechanic, Container<TransferCollection>, ItemCollection {

    public static final int VOLUME_MARK = 1;

    private final Storage volumeStorage = getProfile().getStorageProvider().createStorage(this, RefineryGui.VOLUME_CONTEXT);
    private final Storage filledStorage = getProfile().getStorageProvider().createStorage(this, RefineryGui.FILLED_CONTEXT);
    private final XPDist xpDist = new XPDist(100, 0.3, 1.3);
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
            if (volumeAmount < getVolumeCapacity()
                    && (item == null && collection.has(i -> Volume.getTypeFromMaterial(i.getType()).isPresent()) || item != null && collection.has(item))) {
                int add = this.<RefineryGui>put(collection, getVolumeCapacity() - volumeAmount, getGuiInUse(), RefineryGui::updateAddedVolume, volumeStorage);

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
            if (volume == null || collection.getTransferAmount() < volume.getFluidRequires()) {
                return;
            }

            // check for valid combination
            Optional<Filled> gives = Filled.getFilledState(volume, collection.getFluid());
            if (gives.isEmpty() || (filled != null && !gives.get().equals(filled))) {
                return;
            }

            if ((filled == null || collection.hasFluid(filled.getFluid())) && filledAmount < Refinery.this.getCapacity()) {
                if (filled == null) {
                    // update filled type if not set
                    filled = Filled.getFilledState(volume, collection.getFluid()).orElseThrow(IllegalStateException::new);
                }

                // take fluid after updating filled state if not set
                collection.take(volume.getFluidRequires());

                // update items in gui if in use
                RefineryGui gui = Refinery.this.<RefineryGui>getGuiInUse().get();
                if (gui != null) {
                    gui.updateRemovedVolume(1);
                    gui.updateAddedFilled(1);

                    for (HumanEntity viewer : gui.getInventory().getViewers()) {
                        ((Player)viewer).playSound(viewer.getLocation(), filled.getFillSound(), 0.5f, 0.5f);
                    }
                }

                xp += xpDist.poll() * filled.getVolume().getFluidRequires();

                // update filled amount
                filledAmount++;
                setVolumeAmount(volumeAmount - 1);

                event.setTransferred(true);
            }
        }

        @Override
        public int getCapacity() {
            return -1; // no capacity needed, because no fluid will be stored inside the mechanic
        }
    };

    public Refinery(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
        loadFromStorage();
    }

    @Override
    public void load(MechanicStorageContext context) throws Exception {
        ByteArrayInputStream str = context.getData();

        this.volumeAmount = context.getSerializer().readInt(str);
        ItemStack volume = context.getSerializer().readItemStack(str);
        if (volume != null && this.volumeAmount > 0) {
            this.volume = Volume.getTypeFromMaterial(volume.getType()).orElse(null);
        }

        this.filledAmount = context.getSerializer().readInt(str);
        ItemStack filledStack = context.getSerializer().readItemStack(str);
        if (filledStack != null && this.filledAmount > 0) {
            this.filled = Filled.getFilledStateByStack(filledStack).orElse(null);
        }

        if (this.filledAmount > 0 && this.filled == null) {
            this.filledAmount = 0;
        } else if (this.filledAmount == 0 && this.filled != null) {
            this.filled = null;
        }
        if (this.volumeAmount  > 0 && this.volume == null) {
            this.volumeAmount = 0;
        } else if (this.volumeAmount == 0 && this.volume != null) {
            this.volume = null;
        }
    }

    @Override
    public void save(MechanicStorageContext context) throws IOException, SQLException {
        ByteArrayOutputStream str = new ByteArrayOutputStream();

        context.getSerializer().writeInt(str, this.volumeAmount);
        context.getSerializer().writeItemStack(str,
                Optional.ofNullable(this.volume)
                        .map(Volume::getMat)
                        .map(ItemStack::new)
                        .orElse(null));

        context.getSerializer().writeInt(str, this.filledAmount);
        context.getSerializer().writeItemStack(str,
                Optional.ofNullable(this.filled)
                        .map(Filled::getOutputItemStack)
                        .orElse(null));

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
        return getCapacitySlots(level) *
                Optional.ofNullable(filled)
                        .map(Filled::getOutputItemStack)
                        .map(ItemStack::getMaxStackSize)
                        .orElse(64);
    }

    @Override
    public void pipePut(TransferCollection collection, PipePutEvent event) {
        volumeStorage.ensureValidStorage();

        if (tickThrottle.isThrottled()) {
            return;
        }

        if (collection instanceof ItemCollection itemCollection) {
            delegatedItemContainer.pipePut(itemCollection, event);
        } else if (collection instanceof FluidCollection fluidCollection) {
            delegatedFluidContainer.pipePut(fluidCollection, event);
        }
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
    public double getMaxTransfer() {
        return filled.getOutputItemStack().getMaxStackSize();
    }

    @Override
    public double getTransferAmount() {
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
    public List<ItemStack> pipeTake(int amount) {
        filledStorage.ensureValidStorage();

        if (tickThrottle.isThrottled() || filled == null || filledAmount == 0) {
            return Collections.emptyList();
        }

        return this.<RefineryGui>take((int) Math.min(getMaxTransfer(), amount), filled.getOutputItemStack(), filledAmount, getGuiInUse(), RefineryGui::updateRemovedFilled, filledStorage);
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

    public int getVolumeAmount() {
        return volumeAmount;
    }

    public void setVolumeAmount(int amount) {
        this.volumeAmount = amount;

        if (this.volumeAmount == 0) {
            volume = null;
        }
    }
}
