package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.mechanics.routes.Routes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

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

    @EventHandler
    public void onLeverPull(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.LEVER) {
            for (BlockFace face : Routes.RELATIVES) {
                if (loc.getBlock().getRelative(face).equals(event.getClickedBlock())) {
                    powered = !((Switch)loc.getBlock().getRelative(face).getBlockData()).isPowered();
                    break;
                }
            }
        }
    }

    protected void handleBlockPlace(BlockPlaceEvent event) {
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

    protected void handleBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.LEVER) {
            for (BlockFace face : Routes.RELATIVES) {
                if (loc.getBlock().getRelative(face).equals(event.getBlock())) {
                    levers.remove(event.getBlock());
                    break;
                }
            }
        }
    }

    public abstract void onBlockPlace(BlockPlaceEvent event);

    public abstract void onBlockBreak(BlockBreakEvent event);

    protected void triggerLevers() {
        for (Block lever : levers) {
            triggerLever(lever, powered);
        }
    }
}
