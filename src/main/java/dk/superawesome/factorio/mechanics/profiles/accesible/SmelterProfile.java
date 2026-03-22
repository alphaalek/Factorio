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
import java.util.function.Predicate;

import static dk.superawesome.factorio.util.statics.MathUtil.getIncreaseDifference;
import static dk.superawesome.factorio.util.statics.MathUtil.ticksToMs;
import static dk.superawesome.factorio.util.statics.StringUtil.formatNumber;

public class SmelterProfile implements GuiMechanicProfile<Smelter> {

    private final MechanicFactory<Smelter> factory = new SmelterMechanicFactory();
    private final GuiFactory<Smelter, SmelterGui> guiFactory = new SmelterGuiFactory();

    @Override
    public String getName() {
        return "Smelter";
    }

    @Override
    public Building getBuilding(boolean hasWallSign) {
        return Buildings.SMELTER;
    }

    @Override
    public MechanicFactory<Smelter> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider getStorageProvider() {
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
                    public Predicate<ItemStack> getFilter() {
                        return Smelter::canSmeltStatic;
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
                        return mechanic.getIngredientCapacity();
                    }
                })
                .set(SmelterGui.FUEL_CONTEXT, SmelterGui.FUEL_SLOTS, FuelMechanic::adaptFuelStorage)
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
                .setDescription(2, Arrays.asList("§eLager: 11 stacks §f-> §e15 stacks", "§eBrændstof: 9 stacks §f-> §e16 stacks", "§eIngredienser: 9 stacks §f-> §e16 stacks", "§eHastighed: " + formatNumber(ticksToMs(20)) + "ms §f-> §e" + formatNumber(ticksToMs(19)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(20, 19)*100) +"% hurtigere§f)"))
                .setDescription(3, Arrays.asList("§eLager: 15 stacks §f-> §e22 stacks", "§eBrændstof: 16 stacks §f-> §e24 stacks", "§eIngredienser: 16 stacks §f-> §e24 stacks", "§eHastighed: " + formatNumber(ticksToMs(19)) + "ms §f-> §e" + formatNumber(ticksToMs(18)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(19, 18)*100) +"% hurtigere§f)"))
                .setDescription(4, Arrays.asList("§eLager: 22 stacks §f-> §e32 stacks", "§eBrændstof: 24 stacks §f-> §e36 stacks", "§eIngredienser: 24 stacks §f-> §e36 stacks", "§eHastighed: " + formatNumber(ticksToMs(18)) + "ms §f-> §e" + formatNumber(ticksToMs(16)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(18, 16)*100) +"% hurtigere§f)"))
                .setDescription(5, Arrays.asList("§eLager: 32 stacks §f-> §e64 stacks", "§eBrændstof: 36 stacks §f-> §e54 stacks", "§eIngredienser: 36 stacks §f-> §e54 stacks", "§eHastighed: " + formatNumber(ticksToMs(16)) + "ms §f-> §e" + formatNumber(ticksToMs(14)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(16, 14)*100) +"% hurtigere§f)"))

                .mark(MechanicLevel.XP_REQUIRES_MARK, Array.fromData(1000d, 2500d, 5000d, 10000d))
                .mark(MechanicLevel.LEVEL_COST_MARK, Array.fromData(4096d, 8384d, 18480d, 36200d))

                .mark(MechanicLevel.THINK_DELAY_MARK, Array.fromData(20, 19, 18, 16, 14))

                .mark(ItemCollection.CAPACITY_MARK, Array.fromData(11, 15, 22, 32, 64))
                .mark(Smelter.INGREDIENT_CAPACITY_MARK, Array.fromData(9, 16, 24, 36, 54))
                .mark(Smelter.FUEL_CAPACITY_MARK, Array.fromData(9, 16, 24, 36, 54))
                .build();
    }

    @Override
    public int getID() {
        return 1;
    }

    private static class SmelterMechanicFactory implements MechanicFactory<Smelter> {

        @Override
        public Smelter create(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
            return new Smelter(loc, rotation, context, hasWallSign, isBuild);
        }
    }

    private static class SmelterGuiFactory implements GuiFactory<Smelter, SmelterGui> {

        @Override
        public SmelterGui create(Smelter mechanic, AtomicReference<SmelterGui> inUseReference) {
            return new SmelterGui(mechanic, inUseReference);
        }
    }
}
