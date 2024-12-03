package dk.superawesome.factorio.mechanics.profiles.accesible;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.GuiFactory;
import dk.superawesome.factorio.gui.impl.LiquidTankGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.accessible.LiquidTank;
import dk.superawesome.factorio.mechanics.transfer.FluidCollection;
import dk.superawesome.factorio.util.Array;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class LiquidTankProfile implements GuiMechanicProfile<LiquidTank> {

    private final MechanicFactory<LiquidTank> factory = new LiquidTankMechanicFactory();
    private final GuiFactory<LiquidTank, LiquidTankGui> guiFactory = new LiquidTankGuiFactory();

    @Override
    public String getName() {
        return "Liquid Tank";
    }

    @Override
    public Building getBuilding(boolean hasWallSign) {
        return Buildings.LIQUID_TANK;
    }

    @Override
    public MechanicFactory<LiquidTank> getFactory() {
        return factory;
    }

    @Override
    public GuiFactory<LiquidTank, LiquidTankGui> getGuiFactory() {
        return guiFactory;
    }

    @Override
    public StorageProvider getStorageProvider() {
        return null;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return MechanicLevel.Registry.Builder
                .make(5)
                .setDescription(2, Arrays.asList("§eTank: 100mL §f-> §e250mL"))
                .setDescription(3, Arrays.asList("§eTank: 250mL §f-> §e500mL"))
                .setDescription(4, Arrays.asList("§eTank: 500mL §f-> §e1000mL"))
                .setDescription(5, Arrays.asList("§eTank: 1000mL §f-> §e2500mL"))

                .mark(MechanicLevel.LEVEL_COST_MARK, Array.fromData(6144d, 12288d, 20480d, 51200d))

                .mark(FluidCollection.CAPACITY_MARK, Array.fromData(100, 250, 500, 1000, 2500))
                .build();
    }

    @Override
    public int getID() {
        return 16;
    }

    private static class LiquidTankMechanicFactory implements MechanicFactory<LiquidTank> {

        @Override
        public LiquidTank create(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign) {
            return new LiquidTank(loc, rotation, context, hasWallSign);
        }
    }

    private static class LiquidTankGuiFactory implements GuiFactory<LiquidTank, LiquidTankGui> {

        @Override
        public LiquidTankGui create(LiquidTank mechanic, AtomicReference<LiquidTankGui> inUseReference) {
            return new LiquidTankGui(mechanic, inUseReference);
        }
    }
}
