package dk.superawesome.factorio.mechanics.profiles.power;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.GuiFactory;
import dk.superawesome.factorio.gui.impl.PowerCentralGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.power.PowerCentral;
import dk.superawesome.factorio.util.Array;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static dk.superawesome.factorio.util.statics.MathUtil.getIncreaseDifference;
import static dk.superawesome.factorio.util.statics.MathUtil.ticksToMs;
import static dk.superawesome.factorio.util.statics.StringUtil.formatNumber;

public class PowerCentralProfile implements GuiMechanicProfile<PowerCentral> {

    private final MechanicFactory<PowerCentral> factory = new PowerCentralMechanicFactory();
    private final GuiFactory<PowerCentral, PowerCentralGui> guiFactory = new PowerCentralGuiFactory();

    @Override
    public String getName() {
        return "Power Central";
    }

    @Override
    public Building getBuilding(boolean hasWallSign) {
        return Buildings.POWER_CENTRAL;
    }

    @Override
    public MechanicFactory<PowerCentral> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider getStorageProvider() {
        return null;
    }

    @Override
    public GuiFactory<PowerCentral, PowerCentralGui> getGuiFactory() {
        return guiFactory;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return MechanicLevel.Registry.Builder
                .make(5)
                .setDescription(2, Arrays.asList("§eKapacitet: 1000J §f-> §e5000J", "§eHastighed: " + formatNumber(ticksToMs(20)) + "ms §f-> §e" + formatNumber(ticksToMs(19)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(20, 19)*100) + "% hurtigere§f)"))
                .setDescription(3, Arrays.asList("§eKapacitet: 5000J §f-> §e15000J", "§eHastighed: " + formatNumber(ticksToMs(19)) + "ms §f-> §e" + formatNumber(ticksToMs(18)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(19, 18)*100) + "% hurtigere§f)"))
                .setDescription(4, Arrays.asList("§eKapacitet: 15000J §f-> §e30000J", "§eHastighed: " + formatNumber(ticksToMs(18)) + "ms §f-> §e" + formatNumber(ticksToMs(17)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(18, 17)*100) + "% hurtigere§f)"))
                .setDescription(5, Arrays.asList("§eKapacitet: 30000J §f-> §e50000J", "§eHastighed: " + formatNumber(ticksToMs(17)) + "ms §f-> §e" + formatNumber(ticksToMs(16)) + "ms §f(§e"+ formatNumber(getIncreaseDifference(17, 16)*100) + "% hurtigere§f)"))

                .mark(MechanicLevel.LEVEL_COST_MARK, Array.fromData(6144d, 12288d, 23008d, 46972d))

                .mark(MechanicLevel.THINK_DELAY_MARK, Array.fromData(20, 19, 18, 17, 16))

                .mark(PowerCentral.CAPACITY, Array.fromData(1000d, 5000d, 15000d, 30000d, 50000d))
                .build();
    }

    @Override
    public int getID() {
        return 3;
    }

    private static class PowerCentralMechanicFactory implements MechanicFactory<PowerCentral> {

        @Override
        public PowerCentral create(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
            return new PowerCentral(loc, rotation, context, hasWallSign, isBuild);
        }
    }

    private static class PowerCentralGuiFactory implements GuiFactory<PowerCentral, PowerCentralGui> {

        @Override
        public PowerCentralGui create(PowerCentral mechanic, AtomicReference<PowerCentralGui> inUseReference) {
            return new PowerCentralGui(mechanic, inUseReference);
        }
    }
}
