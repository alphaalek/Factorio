package dk.superawesome.factorio.mechanics.profiles.accesible;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.GuiFactory;
import dk.superawesome.factorio.gui.impl.SmelterGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.accessible.Smelter;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.util.Array;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
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
                .make(5)
                .setDescription(2, Arrays.asList("§eLager: 12 stacks §f-> §e15 stacks", "§eBrændstof: 10 stacks §f-> §e14 stacks", "§eIngredienser: 10 stacks §f-> §e16 stacks"))
                .setDescription(3, Arrays.asList("§eLager: 15 stacks §f-> §e22 stacks", "§eBrændstof: 14 stacks §f-> §e20 stacks", "§eIngredienser: 16 stacks §f-> §e24 stacks"))
                .setDescription(4, Arrays.asList("§eLager: 22 stacks §f-> §e32 stacks", "§eBrændstof: 20 stacks §f-> §e32 stacks", "§eIngredienser: 24 stacks §f-> §e36 stacks"))
                .setDescription(5, Arrays.asList("§eLager: 32 stacks §f-> §e64 stacks", "§eBrændstof: 32 stacks §f-> §e48 stacks", "§eIngredienser: 36 stacks §f-> §e54 stacks"))

                .mark(MechanicLevel.XP_REQUIRES_MARK, Array.fromData(1000d, 2500d, 5000d, 10000d))
                .mark(MechanicLevel.LEVEL_COST_MARK, Array.fromData(4096d, 12288d, 20480d, 51200d))

                .mark(ItemCollection.CAPACITY_MARK, Array.fromData(12, 15, 22, 32, 64))
                .mark(Smelter.INGREDIENT_CAPACITY_MARK, Array.fromData(10, 16, 24, 36, 54))
                .mark(Smelter.FUEL_CAPACITY_MARK, Array.fromData(10, 14, 20, 32, 48))
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
