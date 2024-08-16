package dk.superawesome.factorio.mechanics.impl.behaviour;

import dk.superawesome.factorio.gui.impl.RefineryGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.stackregistry.*;
import dk.superawesome.factorio.mechanics.transfer.FluidCollection;
import dk.superawesome.factorio.mechanics.transfer.FluidContainer;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
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
import java.util.function.Predicate;

public class Refinery extends AbstractMechanic<Refinery> implements AccessibleMechanic, FluidContainer, ItemCollection {

    private static final int VOLUME_MARK = 1;

    private final DelayHandler transferDelayHandler = new DelayHandler(10);

    private int volumeAmount;
    private Volume volume;

    private int filledAmount;
    private Filled filled;

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
        ItemStack filledVolumeStack = context.getSerializer().readItemStack(str);
        int fluidOrdinal = context.getSerializer().readInt(str);
        if (filledVolumeStack != null) {
            Volume filledVolume = Volume.getType(filledVolumeStack.getType()).orElse(null);
            if (filledVolume != null) {
                this.filled = Filled.getFilledState(filledVolume, Fluid.values()[fluidOrdinal]);
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
            context.getSerializer().writeItemStack(str, new ItemStack(this.filled.getVolume().getMat()));
            context.getSerializer().writeInt(str, this.filled.getFluid().ordinal());
        } else {
            context.getSerializer().writeItemStack(str, null);
            context.getSerializer().writeInt(str, -1);
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
    public void pipePut(FluidCollection collection, PipePutEvent event) {
        if (tickThrottle.isThrottled()) {
            return;
        }

        // get bucket or bottle from item collection
        if (collection instanceof ItemCollection itemCollection) {
            if (itemCollection.isTransferEmpty()) {
                return;
            }

            //put(collection, getIngredientCapacity() - ingredientAmount, getGuiInUse(), SmelterGui::updateAddedIngredients, new HeapToStackAccess<>() {
            this.<RefineryGui>put(null, getVolumeCapacity() - volumeAmount, getGuiInUse(), RefineryGui::updateAddedFilled, new HeapToStackAccess<>() {
                @Override
                public FluidStack get() {
                    return null;
                }

                @Override
                public void set(FluidStack val) {
                    setVolumeAmount(volumeAmount + val.getAmount());
                }
            });
        } else {
            // its a fluid collection

        }

        // no empty bottle or not enough fluid to fill a bottle
        if (volume == null || volume.getFluidRequires() > collection.getTransferAmount()) {
            return;
        }
    }

    @Override
    public boolean isTransferEmpty() {
        return filled == null;
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
    public boolean isContainerEmpty() {
        return filledAmount == 0 && volumeAmount == 0;
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d;
    }

    @Override
    public boolean has(ItemStack stack) {
        return has(i -> i.isSimilar(stack) && volumeAmount >= stack.getAmount());
    }

    @Override
    public boolean has(Predicate<ItemStack> stack) {
        return volume != null && stack.test(new ItemStack(volume.getMat()));
    }

    @Override
    public List<ItemStack> take(int amount) {
        if (filled != null) {
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
        return List.of();
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

    public int getVolumeAmount() {
        return volumeAmount;
    }

    public void setVolumeAmount(int amount) {
        this.volumeAmount = amount;

        if (this.volumeAmount == 0) {
            volume = null;
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
    }
}
