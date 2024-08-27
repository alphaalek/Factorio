package dk.superawesome.factorio.mechanics.profiles.accesible;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.GuiFactory;
import dk.superawesome.factorio.gui.impl.EmeraldForgeGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.accessible.EmeraldForge;
import dk.superawesome.factorio.mechanics.transfer.MoneyCollection;
import dk.superawesome.factorio.util.Array;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class EmeraldForgeProfile implements GuiMechanicProfile<EmeraldForge> {

    private final MechanicFactory<EmeraldForge> factory = new EmeraldForgeMechanicFactory();
    private final GuiFactory<EmeraldForge, EmeraldForgeGui> guiFactory = new EmeraldForgeGuiFactory();

    @Override
    public String getName() {
        return "Emerald Forge";
    }

    @Override
    public Building getBuilding() {
        return Buildings.EMERALD_FORGE;
    }

    @Override
    public MechanicFactory<EmeraldForge> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<EmeraldForge> getStorageProvider() {
        return null;
    }

    @Override
    public GuiFactory<EmeraldForge, EmeraldForgeGui> getGuiFactory() {
        return guiFactory;
    }

    @Override
    public MechanicLevel.Registry getLevelRegistry() {
        return MechanicLevel.Registry.Builder
                .make(5)
                .setDescription(2, Arrays.asList("§eLager: 10.000 emeralder §f-> §e50.000 emeralder"))
                .setDescription(3, Arrays.asList("§eLager: 50.000 emeralder §f-> §e100.000 emeralder"))
                .setDescription(4, Arrays.asList("§eLager: 100.000 emeralder §f-> §e250.000 emeralder"))
                .setDescription(5, Arrays.asList("§eLager: 250.000 emeralder §f-> §e500.000 emeralder"))

                .mark(MechanicLevel.LEVEL_COST_MARK, Array.fromData(6144d, 12288d, 20480d, 51200d))

                .mark(MoneyCollection.CAPACITY_MARK, Array.fromData(10_000, 50_000, 100_000, 250_000, 500_000))
                .build();
    }

    @Override
    public int getID() {
        return 5;
    }

    private static class EmeraldForgeMechanicFactory implements MechanicFactory<EmeraldForge> {

        @Override
        public EmeraldForge create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new EmeraldForge(loc, rotation, context);
        }
    }

    private static class EmeraldForgeGuiFactory implements GuiFactory<EmeraldForge, EmeraldForgeGui> {

        @Override
        public EmeraldForgeGui create(EmeraldForge mechanic, AtomicReference<EmeraldForgeGui> inUseReference) {
            return new EmeraldForgeGui(mechanic, inUseReference);
        }
    }
}
