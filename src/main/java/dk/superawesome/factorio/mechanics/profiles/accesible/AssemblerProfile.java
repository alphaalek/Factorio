package dk.superawesome.factorio.mechanics.profiles.accesible;

import dk.superawesome.factorio.api.events.AssemblerTypeChangeEvent;
import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.GuiFactory;
import dk.superawesome.factorio.gui.SingleStorageGui;
import dk.superawesome.factorio.gui.impl.AssemblerGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.accessible.Assembler;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.MoneyCollection;
import dk.superawesome.factorio.util.Array;
import dk.superawesome.factorio.util.statics.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static dk.superawesome.factorio.util.statics.MathUtil.getIncreaseDifference;
import static dk.superawesome.factorio.util.statics.MathUtil.ticksToMs;
import static dk.superawesome.factorio.util.statics.StringUtil.formatNumber;

public class AssemblerProfile implements GuiMechanicProfile<Assembler> {

    private static final MechanicFactory<Assembler> factory = new AssemblerMechanicFactory();
    private static final GuiFactory<Assembler, AssemblerGui> guiFactory = new AssemblerGuiFactory();

    @Override
    public String getName() {
        return "Assembler";
    }

    @Override
    public Building getBuilding(boolean hasWallSign) {
        return Buildings.ASSEMBLER;
    }

    @Override
    public MechanicFactory<Assembler> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider getStorageProvider() {
        return StorageProvider.Builder.<Assembler>makeContext()
                .set(SingleStorageGui.CONTEXT, AssemblerGui.STORAGE_SLOTS, mechanic -> new Storage() {
                    @Override
                    public ItemStack getStored() {
                        return Optional.ofNullable(mechanic.getType())
                                .map(Assembler.Type::getMat)
                                .map(ItemStack::new)
                                .orElse(null);
                    }

                    @Override
                    public Predicate<ItemStack> getFilter() {
                        return item -> Assembler.Types.getTypeFromMaterial(item.getType()).isPresent();
                    }

                    @Override
                    public void setStored(ItemStack stored) {
                        Assembler.Types oldType = Optional.ofNullable(mechanic.getType()).map(Assembler.Type::getType).orElse(null);
                        Optional<Assembler.Types> type = Optional.ofNullable(stored).flatMap(i -> Assembler.Types.getTypeFromMaterial(i.getType()));

                        type.ifPresent(newType -> {
                            mechanic.setType(newType);

                            AssemblerTypeChangeEvent assemblerTypeChangeEvent = new AssemblerTypeChangeEvent(mechanic, oldType, newType);
                            Bukkit.getPluginManager().callEvent(assemblerTypeChangeEvent);
                        });
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
                .build();
    }

    @Override
    public GuiFactory<Assembler, AssemblerGui> getGuiFactory() {
        return guiFactory;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return MechanicLevel.Registry.Builder
                .make(5)
                .setDescription(2, Arrays.asList("§eLager: 7 stacks §f-> §e14 stacks", "§eEmeraldlager: 768 emeralder §f-> §e2.000 emeralder", "§eHastighed: " + formatNumber(ticksToMs(20)) + "ms §f-> §e" + formatNumber(ticksToMs(19)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(20, 19)*100) +"% hurtigere§f)"))
                .setDescription(3, Arrays.asList("§eLager: 14 stacks §f-> §e20 stacks", "§eEmeraldlager: 2.000 emeralder §f-> §e5.000 emeralder", "§eHastighed: " + formatNumber(ticksToMs(20)) + "ms §f-> §e" + formatNumber(ticksToMs(18)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(20, 18)*100) +"% hurtigere§f)"))
                .setDescription(4, Arrays.asList("§eLager: 20 stacks §f-> §e32 stacks", "§eEmeraldlager: 5.000 emeralder §f-> §e10.000 emeralder", "§eHastighed: " + formatNumber(ticksToMs(20)) + "ms §f-> §e" + formatNumber(ticksToMs(17)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(20, 17)*100) +"% hurtigere§f)"))
                .setDescription(5, Arrays.asList("§eLager: 32 stacks §f-> §e64 stacks", "§eEmeraldlager: 10.000 emeralder §f-> §e25.000 emeralder", "§eHastighed: " + formatNumber(ticksToMs(20)) + "ms §f-> §e" + formatNumber(ticksToMs(15)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(20, 15)*100) +"% hurtigere§f)"))

                .mark(MechanicLevel.XP_REQUIRES_MARK, Array.fromData(1500d, 3000d, 7500d, 12500d))
                .mark(MechanicLevel.LEVEL_COST_MARK, Array.fromData(4192d, 10288d, 18480d, 31200d))

                .mark(MechanicLevel.THINK_DELAY_MARK, Array.fromData(20, 19, 18, 17, 15))

                .mark(ItemCollection.CAPACITY_MARK, Array.fromData(7, 14, 20, 32, 64))
                .mark(MoneyCollection.CAPACITY_MARK, Array.fromData(64 * 12d, 2000d, 5000d, 10000d, 25000d))
                .build();
    }

    @Override
    public int getID() {
        return 4;
    }

    private static class AssemblerMechanicFactory implements MechanicFactory<Assembler> {

        @Override
        public Assembler create(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
            return new Assembler(loc, rotation, context, hasWallSign, isBuild);
        }
    }

    private static class AssemblerGuiFactory implements GuiFactory<Assembler, AssemblerGui> {

        @Override
        public AssemblerGui create(Assembler mechanic, AtomicReference<AssemblerGui> inUseReference) {
            return new AssemblerGui(mechanic, inUseReference);
        }
    }
}
