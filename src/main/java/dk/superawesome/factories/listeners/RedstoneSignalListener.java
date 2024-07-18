package dk.superawesome.factories.listeners;

import dk.superawesome.factories.mechanics.pipes.Pipes;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

public class RedstoneSignalListener implements Listener {

    @EventHandler
    public void onRedstoneSignal(BlockRedstoneEvent event) {
        // TODO work only from power central
        if (event.getOldCurrent() == 0) {
            Block block = event.getBlock();
            if (BlockUtil.diode.test(block.getType())) {
                Block pointingBlock = BlockUtil.getPointingBlock(block, true);
                if (pointingBlock != null
                        && BlockUtil.stickyPiston.test(pointingBlock.getType())) {
                    Pipes.suckItems(pointingBlock);
                }
            }
        }
    }
}
