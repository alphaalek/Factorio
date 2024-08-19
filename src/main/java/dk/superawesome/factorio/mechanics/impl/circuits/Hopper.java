package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.api.events.MechanicBuildEvent;
import dk.superawesome.factorio.api.events.MechanicRemoveEvent;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class Hopper extends AbstractMechanic<Hopper> implements ThinkingMechanic {

    private final DelayHandler thinkDelayHandler = new DelayHandler(20);

    private ItemCollection takeMechanic;
    private ItemContainer putMechanic;

    public Hopper(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        // check IO after all other mechanics has been loaded
        Bukkit.getScheduler().runTask(Factorio.get(), this::checkIO);
    }

    @Override
    public MechanicProfile<Hopper> getProfile() {
        return Profiles.HOPPER;
    }

    @Override
    public DelayHandler getThinkDelayHandler() {
        return thinkDelayHandler;
    }

    public void checkIO() {
        MechanicManager manager = Factorio.get().getMechanicManagerFor(this);

        Block up = getLocation().getBlock().getRelative(BlockFace.UP);
        Mechanic<?> takeMechanic = manager.getMechanicPartially(up.getLocation());
        if (takeMechanic instanceof ItemCollection) {
            this.takeMechanic = (ItemCollection) takeMechanic;
        }

        Block hopper = getLocation().getBlock().getRelative(BlockFace.DOWN);
        Mechanic<?> putMechanic = manager.getMechanicPartially(BlockUtil.getPointingBlock(hopper, false).getLocation());
        if (putMechanic instanceof ItemContainer) {
            this.putMechanic = (ItemContainer) putMechanic;
        }
    }

    @EventHandler
    public void onMechanicBuild(MechanicBuildEvent event) {
        Bukkit.getScheduler().runTask(Factorio.get(), this::checkIO);
    }

    @EventHandler
    public void onMechanicRemove(MechanicRemoveEvent event) {
        if (event.getMechanic() == takeMechanic || event.getMechanic() == putMechanic) {
            Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                if (!event.getMechanic().exists()) {
                    if (event.getMechanic() == takeMechanic) {
                        takeMechanic = null;
                    } else if (event.getMechanic() == putMechanic) {
                        putMechanic = null;
                    }
                }
            });
        }
    }

    @Override
    public void think() {
        if (takeMechanic != null && putMechanic != null && !takeMechanic.isTransferEmpty()) {
            putMechanic.pipePut(takeMechanic, new PipePutEvent(null, takeMechanic));
        }
    }
}
