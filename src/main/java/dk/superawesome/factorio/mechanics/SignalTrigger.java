package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.mechanics.routes.Routes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class SignalTrigger<M extends Mechanic<M>> extends AbstractMechanic<M> implements ThinkingMechanic {

    protected final List<Block> levers = new ArrayList<>();

    protected boolean powered;

    public SignalTrigger(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    protected void triggerLever(Block block, boolean powered) {
        Switch lever = (Switch) block.getBlockData();
        lever.setPowered(powered);
        block.setBlockData(lever);
    }

    protected void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.LEVER) {
            for (BlockFace face : Routes.RELATIVES) {
                if (loc.getBlock().getRelative(face).equals(event.getBlock())) {
                    levers.add(event.getBlock());

                    if (powered) {
                        triggerLever(event.getBlock(), true);
                    }
                    break;
                }
            }
        }
    }

    protected void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.LEVER) {
            for (BlockFace face : Routes.RELATIVES) {
                if (loc.getBlock().getRelative(face).equals(event.getBlock())) {
                    levers.remove(event.getBlock());
                    break;
                }
            }
        }
    }
}
