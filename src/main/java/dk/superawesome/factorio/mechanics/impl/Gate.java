package dk.superawesome.factorio.mechanics.impl;

import dk.superawesome.factorio.mechanics.AbstractMechanic;
import dk.superawesome.factorio.mechanics.MechanicProfile;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.mechanics.Profiles;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.routes.events.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockRedstoneEvent;

public class Gate extends AbstractMechanic<Gate> implements ItemContainer {

    private boolean open = true;

    public Gate(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public MechanicProfile<Gate> getProfile() {
        return Profiles.GATE;
    }

    @Override
    public void onBlocksLoaded() {
        for (BlockFace face : Routes.SIGNAL_EXPAND_DIRECTIONS) {
            Block block = loc.getBlock().getRelative(face);
            if (block.getType() == Material.REPEATER) {
                if (BlockUtil.getPointingBlock(block, true).getLocation().equals(loc)) {
                    if (((Repeater)block.getBlockData()).isPowered()) {
                        this.open = false;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onRedstoneInput(BlockRedstoneEvent event) {
        if (event.getBlock().getType() == Material.REPEATER) {
            if (BlockUtil.getPointingBlock(event.getBlock(), true).getLocation().equals(loc)) {
                this.open = event.getNewCurrent() == 0;
            }
        }
    }

    @Override
    public boolean isContainerEmpty() {
        return true;
    }

    @Override
    public void pipePut(ItemCollection collection, PipePutEvent event) {
        // only continue pipe route if the gate is open
        if (open) {
            if (Routes.startTransferRoute(loc.getBlock(), collection)) {
                event.setTransferred(true);
            }
        }
    }

    @Override
    public int getCapacity() {
        return -1;
    }
}
