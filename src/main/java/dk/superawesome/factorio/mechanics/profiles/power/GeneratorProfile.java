package dk.superawesome.factorio.mechanics.profiles.power;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.GuiFactory;
import dk.superawesome.factorio.gui.SingleStorageGui;
import dk.superawesome.factorio.gui.impl.GeneratorGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.power.Generator;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.util.Array;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static dk.superawesome.factorio.util.statics.MathUtil.getIncreaseDifference;
import static dk.superawesome.factorio.util.statics.MathUtil.ticksToMs;
import static dk.superawesome.factorio.util.statics.StringUtil.formatNumber;

public class GeneratorProfile implements GuiMechanicProfile<Generator> {

    private static final MechanicFactory<Generator> factory = new GeneratorMechanicFactory();
    private static final GuiFactory<Generator, GeneratorGui> guiFactory = new GeneratorGuiFactory();

    @Override
    public String getName() {
        return "Generator";
    }

    @Override
    public Building getBuilding(boolean hasWallSign) {
        return Buildings.GENERATOR;
    }

    @Override
    public MechanicFactory<Generator> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider getStorageProvider() {
        return StorageProvider.Builder.<Generator>makeContext()
                .set(SingleStorageGui.CONTEXT, GeneratorGui.STORAGE_SLOTS, FuelMechanic::adaptFuelStorage)
                .build();
    }

    @Override
    public GuiFactory<Generator, GeneratorGui> getGuiFactory() {
        return guiFactory;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return MechanicLevel.Registry.Builder
                .make(5)
                .setDescription(2, Arrays.asList("§eLager: 23 stacks §f-> §e48 stacks", "§eHastighed: " + formatNumber(ticksToMs(20)) + "ms §f-> §e" + formatNumber(ticksToMs(19)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(20, 19)*100) +"% hurtigere§f)"))
                .setDescription(3, Arrays.asList("§eLager: 48 stacks §f-> §e96 stacks", "§eHastighed: " + formatNumber(ticksToMs(19)) + "ms §f-> §e" + formatNumber(ticksToMs(18)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(19, 18)*100) +"% hurtigere§f)"))
                .setDescription(4, Arrays.asList("§eLager: 96 stacks §f-> §e162 stacks", "§eHastighed: " + formatNumber(ticksToMs(18)) + "ms §f-> §e" + formatNumber(ticksToMs(17)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(18, 17)*100) +"% hurtigere§f)"))
                .setDescription(5, Arrays.asList("§eLager: 162 stacks §f-> §e256 stacks", "§eHastighed: " + formatNumber(ticksToMs(17)) + "ms §f-> §e" + formatNumber(ticksToMs(16)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(17, 16)*100) +"% hurtigere§f)"))

                .mark(MechanicLevel.XP_REQUIRES_MARK, Array.fromData(2500d, 7500d, 15000d, 30000d))
                .mark(MechanicLevel.LEVEL_COST_MARK, Array.fromData(6144d, 12288d, 43008d, 81920d))

                .mark(MechanicLevel.THINK_DELAY_MARK, Array.fromData(20, 19, 18, 17, 16))
                .mark(ItemCollection.CAPACITY_MARK, Array.fromData(23, 48, 96, 162, 256))
                .build();
    }

    @Override
    public int getID() {
        return 6;
    }

    private static class GeneratorMechanicFactory implements MechanicFactory<Generator> {

        @Override
        public Generator create(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
            return new Generator(loc, rotation, context, hasWallSign, isBuild);
        }
    }

    private static class GeneratorGuiFactory implements GuiFactory<Generator, GeneratorGui> {

        @Override
        public GeneratorGui create(Generator mechanic, AtomicReference<GeneratorGui> inUseReference) {
            return new GeneratorGui(mechanic, inUseReference);
        }
    }
}
