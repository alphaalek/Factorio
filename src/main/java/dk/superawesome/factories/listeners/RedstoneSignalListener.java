package dk.superawesome.factories.listeners;

import dk.superawesome.factories.production.pipes.Pipes;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

public class RedstoneSignalListener implements Listener {

    @EventHandler
    public void onRedstoneSignal(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        if (BlockUtil.diode.is(block)) {
            Block pointingBlock = BlockUtil.getPointingBlock(block);
            if (pointingBlock != null
                    && BlockUtil.diode.is(pointingBlock)) {
                Pipes.suckItems(pointingBlock);
            }
        }
    }
}
