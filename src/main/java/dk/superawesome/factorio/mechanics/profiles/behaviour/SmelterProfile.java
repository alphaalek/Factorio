package dk.superawesome.factorio.mechanics.profiles.behaviour;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.GuiFactory;
import dk.superawesome.factorio.gui.impl.SmelterGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.behaviour.Smelter;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.util.Array;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.atomic.AtomicReference;

public class SmelterProfile implements GuiMechanicProfile<Smelter> {

    private final MechanicFactory<Smelter> factory = new SmelterMechanicFactory();
    private final GuiFactory<Smelter, SmelterGui> guiFactory = new SmelterGuiFactory();

    @Override
    public String getName() {
        return "Smelter";
    }

    @Override
    public Building getBuilding() {
        return Buildings.SMELTER;
    }

    @Override
    public MechanicFactory<Smelter> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<Smelter> getStorageProvider() {
        return StorageProvider.Builder.<Smelter>makeContext()
                .set(SmelterGui.INGREDIENT_CONTEXT, SmelterGui.INGREDIENT_SLOTS, mechanic -> new Storage() {
                    @Override
                    public ItemStack getStored() {
                        return mechanic.getIngredient();
                    }

                    @Override
                    public void setStored(ItemStack stored) {
                        mechanic.setIngredient(stored);
                    }

                    @Override
                    public int getAmount() {
                        return mechanic.getIngredientAmount();
                    }

                    @Override
                    public void setAmount(int amount) {
                        mechanic.setIngredientAmount(amount);
                    }

                    @Override
                    public int getCapacity() {
                        return mechanic.getCapacity();
                    }
                })
                .set(SmelterGui.FUEL_CONTEXT, SmelterGui.FUEL_SLOTS, FuelMechanic::convertFuelStorage)
                .set(SmelterGui.STORED_CONTEXT, SmelterGui.STORAGE_SLOTS, mechanic -> new Storage() {
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
    public GuiFactory<Smelter, SmelterGui> getGuiFactory() {
        return guiFactory;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return MechanicLevel.Registry.Builder
                .make(1)
                .mark(ItemCollection.CAPACITY_MARK, Array.fromData(12))
                .mark(Smelter.INGREDIENT_CAPACITY_MARK, Array.fromData(10))
                .mark(Smelter.FUEL_CAPACITY_MARK, Array.fromData(10))
                .build();
    }

    @Override
    public int getID() {
        return 1;
    }

    private static class SmelterMechanicFactory implements MechanicFactory<Smelter> {

        @Override
        public Smelter create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new Smelter(loc, rotation, context);
        }
    }

    private static class SmelterGuiFactory implements GuiFactory<Smelter, SmelterGui> {

        @Override
        public SmelterGui create(Smelter mechanic, AtomicReference<SmelterGui> inUseReference) {
            return new SmelterGui(mechanic, inUseReference);
        }
    }
}
