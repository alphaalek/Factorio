package dk.superawesome.factorio.mechanics;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public interface Lightable {

    void updateLight();

    static void setLit(Block block, boolean lit) {
        BlockData data = block.getBlockData();
        if (data instanceof org.bukkit.block.data.Lightable) {
            ((org.bukkit.block.data.Lightable)data).setLit(lit);
            block.setBlockData(data);
        }
    }
}
