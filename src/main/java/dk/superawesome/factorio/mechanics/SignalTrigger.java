package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.routes.Routes;
import org.bukkit.Bukkit;
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

public abstract class SignalTrigger<M extends Mechanic<M>> extends AbstractMechanic<M> {

    protected final List<Block> levers = new ArrayList<>();

    protected boolean powered;

    public SignalTrigger(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    protected void triggerLever(Block block, boolean powered) {
        this.powered = powered;
        if (block.getType() == Material.LEVER) {
            Switch lever = (Switch) block.getBlockData();
            lever.setPowered(powered);
            block.setBlockData(lever);
        } else {
            levers.remove(block);
        }
    }

    protected void setupRelativeBlocks(Consumer<Block> findBlock, Consumer<Mechanic<?>> findMechanic) {
        MechanicManager manager = Factorio.get().getMechanicManagerFor(this);

        for (BlockFace face : Routes.RELATIVES) {
            Block block = loc.getBlock().getRelative(face);
            if (block.getType() == Material.LEVER) {
                levers.add(block);
                findBlock.accept(block);
            }

            Mechanic<?> at = manager.getMechanicPartially(block.getLocation());
            if (at != null) {
                findMechanic.accept(at);
            }
        }

        triggerLevers();
    }

    protected void handleLeverPull(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.LEVER && levers.contains(event.getClickedBlock())) {
            powered = !((Switch)event.getClickedBlock().getBlockData()).isPowered();
        }
    }

    protected void handleBlockBreak(BlockBreakEvent event) {
        levers.remove(event.getBlock());
    }

    protected void handleBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.LEVER) {
            for (BlockFace face : Routes.RELATIVES) {
                if (loc.getBlock().getRelative(face).equals(event.getBlock())) {
                    levers.add(event.getBlock());

                    triggerLever(event.getBlock(), powered);
                    break;
                }
            }
        }
    }

    public boolean isPowered() {
        return powered;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
    }

    public abstract void onBlockPlace(BlockPlaceEvent event);

    public abstract void onBlockBreak(BlockBreakEvent event);

    public abstract void onLeverPull(PlayerInteractEvent event);

    protected void triggerLevers(boolean powered) {
        this.powered = powered;
        for (Block lever : new ArrayList<>(levers)) {
            triggerLever(lever, powered);
        }
    }

    protected void triggerLevers() {
        triggerLevers(powered);
    }
}
