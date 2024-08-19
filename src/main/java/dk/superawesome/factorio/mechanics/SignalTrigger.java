package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.routes.Routes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

    protected void setupRelativeBlocks(Consumer<Mechanic<?>> find) {
        MechanicManager manager = Factorio.get().getMechanicManagerFor(this);

        for (BlockFace face : Routes.RELATIVES) {
            Block block = loc.getBlock().getRelative(face);
            if (block.getType() == Material.LEVER) {
                levers.add(block);
                triggerLever(block, true); // locked so assembler types can be updated before triggering
            }

            Mechanic<?> at = manager.getMechanicPartially(block.getLocation());
            if (at != null) {
                find.accept(at);
            }
        }
    }

    protected void handleLeverPull(PlayerInteractEvent event) {
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

    public abstract void onLeverPull(PlayerInteractEvent event);

    protected void triggerLevers() {
        for (Block lever : levers) {
            triggerLever(lever, powered);
        }
    }
}
