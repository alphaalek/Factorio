package dk.superawesome.factorio.mechanics.profiles.behaviour;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.GuiFactory;
import dk.superawesome.factorio.gui.impl.RefineryGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.behaviour.Refinery;
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
            .set(RefineryGui.EMPTY_BOTTLE_CONTEXT, RefineryGui.BOTTLES_SLOTS, mechanic -> new Storage() {
                @Override
                public ItemStack getStored() {
                    return mechanic.getBottleType();
                }

                @Override
                public void setStored(ItemStack stored) {
                    mechanic.setBottleType(stored);
                }

                @Override
                public int getAmount() {
                    return mechanic.getBottleAmount();
                }

                @Override
                public void setAmount(int amount) {
                    mechanic.setBottleAmount(amount);
                }

                @Override
                public int getCapacity() {
                    return mechanic.getCapacity();
                }
            })
            .set(RefineryGui.FILLED_BOTTLE_CONTEXT, RefineryGui.FILLED_BOTTLES_SLOTS, mechanic -> new Storage() {
                @Override
                public ItemStack getStored() {
                    return mechanic.getStorageType();
                }

                @Override
                public void setStored(ItemStack stored) {
                    mechanic.setStorageType(stored);
                }

                @Override
                public int getAmount() {
                    return mechanic.getStorageAmount();
                }

                @Override
                public void setAmount(int amount) {
                    mechanic.setStorageAmount(amount);
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
