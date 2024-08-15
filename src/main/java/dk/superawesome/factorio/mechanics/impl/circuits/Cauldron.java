package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.transfer.FluidCollection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.bukkit.event.block.CauldronLevelChangeEvent.ChangeReason.UNKNOWN;

public class Cauldron extends AbstractMechanic<Cauldron> implements FluidCollection {

    private int level = 0;
    private ItemStack fluid = null;

    public Cauldron(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public MechanicProfile<Cauldron> getProfile() {
        return Profiles.CAULDRON;
    }

    @EventHandler
    public void onCauldronLevelChange(CauldronLevelChangeEvent event) {
        if (event.getBlock().getLocation().equals(loc)) {
            this.level = event.getNewLevel();
            if (level == 0) this.fluid = null;
            else
                this.fluid = event.getNewState().getType() == Material.WATER_CAULDRON ? new ItemStack(Material.WATER) : new ItemStack(Material.LAVA);
        }
    }

    @Override
    public void onBlocksLoaded() {
        Bukkit.getPluginManager().callEvent(
            new CauldronLevelChangeEvent(loc.getBlock(), null, UNKNOWN, loc.getBlock().getState())
        );
    }

    @Override
    public List<ItemStack> take(int amount) {
        ItemStack item = fluid.clone();
        if (level > amount) {
            item.setAmount(amount);
        }

        level = Math.max(0, level - item.getAmount());
        if (level == 0) {
            fluid = null;
        } else {
            fluid.setAmount(level);
            updateCauldronLevel();
        }

        return Collections.singletonList(item);
    }

    @Override
    public boolean hasFluid(FluidType fluidType) {
        return fluid != null && fluid.getType().equals(fluidType.getMaterial());
    }

    @Override
    public boolean hasFluid(Predicate<FluidType> fluidType) {
        return fluid != null && fluidType.test(FluidType.valueOf(fluid.getType().name()));
    }

    private void updateCauldronLevel() {
        Block block = loc.getBlock();
        BlockData data = block.getBlockData();
        ((Levelled) data).setLevel(level);
        block.setBlockData(data);
        block.getState().update();
    }

    @Override
    public boolean isTransferEmpty() {
        return level == 0;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return DelayHandler.NO_DELAY;
    }

    @Override
    public int getMaxTransfer() {
        return fluid.getMaxStackSize();
    }

    @Override
    public int getTransferAmount() {
        return level;
    }

    @Override
    public double getTransferEnergyCost() {
        return 1d / 4d;
    }
}
