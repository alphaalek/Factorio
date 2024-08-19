package dk.superawesome.factorio.mechanics.profiles.behaviour;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.GuiFactory;
import dk.superawesome.factorio.gui.impl.LiquidTankGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.behaviour.LiquidTank;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.concurrent.atomic.AtomicReference;

public class LiquidTankProfile implements GuiMechanicProfile<LiquidTank> {

    private final MechanicFactory<LiquidTank> factory = new LiquidTankMechanicFactory();
    private final GuiFactory<LiquidTank, LiquidTankGui> guiFactory = new LiquidTankGuiFactory();

    @Override
    public String getName() {
        return "Liquid Tank";
    }

    @Override
    public Building getBuilding() {
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
    public StorageProvider<LiquidTank> getStorageProvider() {
        return null;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return null;
    }

    @Override
    public int getID() {
        return 16;
    }

    private static class LiquidTankMechanicFactory implements MechanicFactory<LiquidTank> {

        @Override
        public LiquidTank create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new LiquidTank(loc, rotation, context);
        }
    }

    private static class LiquidTankGuiFactory implements GuiFactory<LiquidTank, LiquidTankGui> {

        @Override
        public LiquidTankGui create(LiquidTank mechanic, AtomicReference<LiquidTankGui> inUseReference) {
            return new LiquidTankGui(mechanic, inUseReference);
        }
    }
}
