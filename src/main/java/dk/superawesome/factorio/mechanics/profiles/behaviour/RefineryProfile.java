package dk.superawesome.factorio.mechanics.profiles.behaviour;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.GuiFactory;
import dk.superawesome.factorio.gui.impl.RefineryGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.behaviour.Refinery;
import dk.superawesome.factorio.mechanics.stackregistry.Filled;
import dk.superawesome.factorio.mechanics.stackregistry.Volume;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.util.Array;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class RefineryProfile implements GuiMechanicProfile<Refinery> {

    private static final MechanicFactory<Refinery> factory = new RefineryMechanicFactory();
    private static final GuiFactory<Refinery, RefineryGui> guiFactory = new RefineryGuiFactory();

    @Override
    public String getName() {
        return "Refinery";
    }

    @Override
    public Building getBuilding() {
        return Buildings.REFINERY;
    }

    @Override
    public MechanicFactory<Refinery> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<Refinery> getStorageProvider() {
        return StorageProvider.Builder.<Refinery>makeContext()
            .set(RefineryGui.VOLUME_CONTEXT, RefineryGui.VOLUME_SLOTS, mechanic -> new Storage() {
                @Override
                public ItemStack getStored() {
                    return Optional.ofNullable(mechanic.getVolume())
                            .map(Volume::getMat)
                            .map(ItemStack::new)
                            .orElse(null);
                }

                @Override
                public void setStored(ItemStack stored) {
                    Volume.getTypeFromMaterial(stored.getType()).ifPresent(mechanic::setVolume);
                }

                @Override
                public int getAmount() {
                    return mechanic.getVolumeAmount();
                }

                @Override
                public void setAmount(int amount) {
                    mechanic.setVolumeAmount(amount);
                }

                @Override
                public int getCapacity() {
                    return mechanic.getVolumeCapacity();
                }
            })
            .set(RefineryGui.FILLED_CONTEXT, RefineryGui.FILLED_SLOTS, mechanic -> new Storage() {
                @Override
                public ItemStack getStored() {
                    return Optional.ofNullable(mechanic.getFilled()).
                        map(Filled::getOutputItemStack).
                        orElse(null);
                }

                @Override
                public void setStored(ItemStack stored) {
                    Filled.getFilledStateByStack(stored).ifPresent(mechanic::setFilled);
                }

                @Override
                public int getAmount() {
                    return mechanic.getFilledAmount();
                }

                @Override
                public void setAmount(int amount) {
                    mechanic.setFilledAmount(amount);
                }

                @Override
                public int getCapacity() {
                    return mechanic.getCapacity();
                }
            })
            .build();
    }

    @Override
    public GuiFactory<Refinery, RefineryGui> getGuiFactory() {
        return guiFactory;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return MechanicLevel.Registry.Builder
                .make(1)
                .mark(ItemCollection.CAPACITY_MARK, Array.fromData(12))
                .mark(Refinery.VOLUME_MARK, Array.fromData(8))
                .build();
    }

    @Override
    public int getID() {
        return 13;
    }

    private static class RefineryMechanicFactory implements MechanicFactory<Refinery> {

        @Override
        public Refinery create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new Refinery(loc, rotation, context);
        }
    }

    private static class RefineryGuiFactory implements GuiFactory<Refinery, RefineryGui> {

        @Override
        public RefineryGui create(Refinery mechanic, AtomicReference<RefineryGui> inUseReference) {
            return new RefineryGui(mechanic, inUseReference);
        }
    }
}
