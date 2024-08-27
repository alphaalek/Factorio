package dk.superawesome.factorio.mechanics.profiles.accesible;

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
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
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
                .make(5)
                .setDescription(2, Arrays.asList("§eLager: 8 stacks §f-> §e14 stacks", "§eEmeralder: 768 emeralder §f-> §e2000 emeralder"))
                .setDescription(3, Arrays.asList("§eLager: 14 stacks §f-> §e20 stacks", "§eEmeralder: 2000 emeralder §f-> §e5000 emeralder"))
                .setDescription(4, Arrays.asList("§eLager: 20 stacks §f-> §e32 stacks", "§eEmeralder: 5000 emeralder §f-> §e10000 emeralder"))
                .setDescription(5, Arrays.asList("§eLager: 32 stacks §f-> §e64 stacks", "§eEmeralder: 10000 emeralder §f-> §e25000 emeralder"))

                .mark(MechanicLevel.XP_REQUIRES_MARK, Array.fromData(1500d, 3000d, 7500d, 12500d))
                .mark(MechanicLevel.LEVEL_COST_MARK, Array.fromData(6144d, 12288d, 20480d, 51200d))

                .mark(ItemCollection.CAPACITY_MARK, Array.fromData(8, 14, 20, 32, 64))
                .mark(MoneyCollection.CAPACITY_MARK, Array.fromData(64 * 12d, 2000d, 5000, 10000, 25000))
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
