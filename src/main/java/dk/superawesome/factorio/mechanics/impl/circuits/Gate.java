package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.TransferCollection;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

public class Gate extends Circuit<Gate, TransferCollection> implements Container<TransferCollection> {

    private boolean open = true;

    public Gate(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public MechanicProfile<Gate> getProfile() {
        return Profiles.GATE;
    }

    private void checkSignal() {
        this.open = true;
        for (BlockFace face : Routes.SIGNAL_EXPAND_DIRECTIONS) {
            Block block = loc.getBlock().getRelative(face);
            if (block.getType() == Material.REPEATER &&
                    BlockUtil.getPointingBlock(block, true).getLocation().equals(loc)
                    && ((Repeater)block.getBlockData()).isPowered()) {
                this.open = false;
            }
        }
    }

    @Override
    public void onBlocksLoaded(Player by) {
        checkSignal();
    }

    @EventHandler
    public void onRedstoneInput(BlockRedstoneEvent event) {
        if (event.getBlock().getType() == Material.REPEATER
                && BlockUtil.getPointingBlock(event.getBlock(), true).getLocation().equals(loc)) {
            this.open = event.getNewCurrent() == 0;
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (BlockUtil.isRelativeFast(event.getBlock(), loc.getBlock())) {
            Bukkit.getScheduler().runTask(Factorio.get(), this::checkSignal);
        }
    }

    @Override
    public boolean accepts(TransferCollection collection) {
        return true;
    }

    @Override
    public boolean isContainerEmpty() {
        return true;
    }

    @Override
    public boolean pipePut(TransferCollection collection) {
        return open && Routes.startTransferRoute(loc.getBlock(), collection, this, false);
    }

    @Override
    public int getCapacity() {
        return -1;
    }
}
