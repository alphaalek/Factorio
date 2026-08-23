package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.impl.relative.Collector;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;

import java.util.List;

public interface WasteOutputMechanic {

    List<BlockVector> getWasteOutputs();

    Location getLocation();

    default void handleWaste(Location def, Material waste) {
        MechanicManager manager = Factorio.get().getMechanicManager(def.getWorld());

        for (BlockVector vec : getWasteOutputs()) {
            // search for collectors to take the waste
            Location loc = BlockUtil.getRel(def, vec);
            Block block = loc.getBlock();
            if (block.getType() == Material.HOPPER) {
                // check if the hopper is facing towards the mechanic
                if (manager.getMechanicAt(BlockUtil.getPointingBlock(block, false).getLocation()) == this) {
                    Mechanic<?> mechanic = manager.getMechanicAt(loc);
                    if (mechanic instanceof Collector collector) {
                        if (collector.handleInput(waste)) {
                            // the collector took the waste, just break
                            break;
                        }
                    }
                }
            }
        }
    }
}
