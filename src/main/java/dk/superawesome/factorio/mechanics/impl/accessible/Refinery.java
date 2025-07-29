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
    }

    @Override
    public void loadData(ByteArrayInputStream data) throws Exception {
        this.volumeAmount = this.context.getSerializer().readInt(data);
        ItemStack volume = this.context.getSerializer().readItemStack(data);
        if (volume != null && this.volumeAmount > 0) {
            this.volume = Volume.getTypeFromMaterial(volume.getType()).orElse(null);
        }

        this.filledAmount = this.context.getSerializer().readInt(data);
        ItemStack filledStack = this.context.getSerializer().readItemStack(data);
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
    public Optional<ByteArrayOutputStream> saveData() throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        this.context.getSerializer().writeInt(data, this.volumeAmount);
        this.context.getSerializer().writeItemStack(data,
                Optional.ofNullable(this.volume)
                        .map(Volume::getMat)
                        .map(ItemStack::new)
                        .orElse(null));

        this.context.getSerializer().writeInt(data, this.filledAmount);
        this.context.getSerializer().writeItemStack(data,
                Optional.ofNullable(this.filled)
                        .map(Filled::getOutputItemStack)
                        .orElse(null));

        return Optional.of(data);
    }

    @Override
    public MechanicProfile<Refinery> getProfile() {
        return Profiles.REFINERY;
    }

    public int getVolumeCapacity() {
        return this.level.getInt(VOLUME_MARK) *
                Optional.ofNullable(this.volume)
                        .map(Volume::getMat)
                        .map(Material::getMaxStackSize)
                        .orElse(64);
    }

    @Override
    public int getCapacity() {
        return getCapacitySlots(this.level) *
                Optional.ofNullable(this.filled)
                        .map(Filled::getOutputItemStack)
                        .map(ItemStack::getMaxStackSize)
                        .orElse(64);
    }

    @Override
    public void pipePut(TransferCollection collection, PipePutEvent event) {
        this.volumeStorage.ensureValidStorage();

        if (this.tickThrottle.isThrottled()) {
            return;
        }

        if (collection instanceof ItemCollection itemCollection) {
            this.delegatedItemContainer.pipePut(itemCollection, event);
        } else if (collection instanceof FluidCollection fluidCollection) {
            this.delegatedFluidContainer.pipePut(fluidCollection, event);
        }
    }

    @Override
    public boolean isTransferEmpty() {
        return this.filledAmount == 0;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return this.transferDelayHandler;
    }

    @Override
    public double getMaxTransfer() {
        return this.filled.getOutputItemStack().getMaxStackSize();
    }

    @Override
    public double getTransferAmount() {
        return this.filledAmount;
    }

    @Override
    public boolean accepts(TransferCollection collection) {
        return collection instanceof ItemCollection || collection instanceof FluidCollection;
    }

    @Override
    public boolean isContainerEmpty() {
        return this.delegatedFluidContainer.isContainerEmpty() && this.delegatedItemContainer.isContainerEmpty();
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d;
    }

    @Override
    public boolean has(ItemStack stack) {
        return has(i -> i.isSimilar(stack) && this.filledAmount >= stack.getAmount());
    }

    @Override
    public boolean has(Predicate<ItemStack> stack) {
        return this.filled != null && stack.test(this.filled.getOutputItemStack());
    }

    @Override
    public List<ItemStack> pipeTake(int amount) {
        this.filledStorage.ensureValidStorage();

        if (this.tickThrottle.isThrottled() || this.filled == null || this.filledAmount == 0) {
            return Collections.emptyList();
        }

        return this.<RefineryGui>take((int) Math.min(getMaxTransfer(), amount), this.filled.getOutputItemStack(), this.filledAmount, getGuiInUse(), RefineryGui::updateRemovedFilled, this.filledStorage);
    }

    public Volume getVolume() {
        return this.volume;
    }

    public void setVolume(Volume volume) {
        this.volume = volume;

        if (this.volume == null) {
            this.volumeAmount = 0;
        }
    }

    public Filled getFilled() {
        return this.filled;
    }

    public void setFilled(Filled filled) {
        this.filled = filled;

        if (this.filled == null) {
            this.filledAmount = 0;
        }
    }

    public int getFilledAmount() {
        return this.filledAmount;
    }

    public void setFilledAmount(int amount) {
        this.filledAmount = amount;

        if (this.filledAmount == 0) {
            this.filled = null;
        }
    }

    public int getVolumeAmount() {
        return this.volumeAmount;
    }

    public void setVolumeAmount(int amount) {
        this.volumeAmount = amount;

        if (this.volumeAmount == 0) {
            this.volume = null;
        }
    }
}
