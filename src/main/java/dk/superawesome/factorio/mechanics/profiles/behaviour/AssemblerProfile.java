package dk.superawesome.factorio.mechanics.profiles.behaviour;

import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.gui.GuiFactory;
import dk.superawesome.factorio.gui.SingleStorageGui;
import dk.superawesome.factorio.gui.impl.AssemblerGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.behaviour.Assembler;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.MoneyCollection;
import dk.superawesome.factorio.util.Array;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class AssemblerProfile implements GuiMechanicProfile<Assembler> {

    private static final MechanicFactory<Assembler> factory = new AssemblerMechanicFactory();
    private static final GuiFactory<Assembler, AssemblerGui> guiFactory = new AssemblerGuiFactory();

    @Override
    public String getName() {
        return "Assembler";
    }

    @Override
    public Building getBuilding() {
        return Buildings.ASSEMBLER;
    }

    @Override
    public MechanicFactory<Assembler> getFactory() {
        return factory;
    }

    @Override
    public StorageProvider<Assembler> getStorageProvider() {
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
                    public void setStored(ItemStack stored) {
                        Optional<Assembler.Types> type = Assembler.Types.getTypeFromMaterial(stored.getType());
                        type.ifPresent(mechanic::setType);
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
                .make(1)
                .mark(ItemCollection.CAPACITY_MARK, Array.fromData(8))
                .mark(MoneyCollection.CAPACITY_MARK, Array.fromData(64 * 12d))
                .build();
    }

    @Override
    public int getID() {
        return 4;
    }

    private static class AssemblerMechanicFactory implements MechanicFactory<Assembler> {

        @Override
        public Assembler create(Location loc, BlockFace rotation, MechanicStorageContext context) {
            return new Assembler(loc, rotation, context);
        }
    }

    private static class AssemblerGuiFactory implements GuiFactory<Assembler, AssemblerGui> {

        @Override
        public AssemblerGui create(Assembler mechanic, AtomicReference<AssemblerGui> inUseReference) {
            return new AssemblerGui(mechanic, inUseReference);
        }
    }
}
