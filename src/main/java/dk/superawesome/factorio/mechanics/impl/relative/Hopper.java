package dk.superawesome.factorio.mechanics.impl.relative;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.api.events.MechanicBuildEvent;
import dk.superawesome.factorio.api.events.MechanicLoadEvent;
import dk.superawesome.factorio.api.events.MechanicRemoveEvent;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class Hopper extends AbstractMechanic<Hopper> implements ThinkingMechanic {

    private final DelayHandler thinkDelayHandler = new DelayHandler(20);

    private ItemCollection takeMechanic;
    private ItemContainer putMechanic;

    public Hopper(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        this.takeMechanic = null;
        this.putMechanic = null;
        // check IO after all other mechanics has been loaded
        Bukkit.getScheduler().runTask(Factorio.get(), this::checkIO);
    }

    @Override
    public MechanicProfile<Hopper> getProfile() {
        return Profiles.HOPPER;
    }

    @Override
    public DelayHandler getThinkDelayHandler() {
        return this.thinkDelayHandler;
    }

    public void checkIO() {
        MechanicManager manager = Factorio.get().getMechanicManagerFor(this);

        Block hopper = this.loc.getBlock().getRelative(BlockFace.DOWN);
        if (hopper.getType() != Material.HOPPER) {
            // invalid hopper
            manager.deleteMechanic(this);
            return;
        }

        Block up = this.loc.getBlock().getRelative(BlockFace.UP);
        Mechanic<?> takeMechanic = manager.getMechanicAt(up.getLocation());
        if (takeMechanic instanceof ItemCollection) {
            this.takeMechanic = (ItemCollection) takeMechanic;
        }

        Mechanic<?> putMechanic = manager.getMechanicAt(BlockUtil.getPointingBlock(hopper, false).getLocation());
        if (putMechanic instanceof ItemContainer) {
            this.putMechanic = (ItemContainer) putMechanic;
        }
    }

    @EventHandler
    public void onMechanicBuild(MechanicBuildEvent event) {
        if (isRelevantMechanic(event.getMechanic())) {
            Bukkit.getScheduler().runTask(Factorio.get(), this::checkIO);
        }
    }

    @EventHandler
    public void onMechanicLoad(MechanicLoadEvent event) {
        if (isRelevantMechanic(event.getMechanic())) {
            Bukkit.getScheduler().runTask(Factorio.get(), this::checkIO);
        }
    }

    private boolean isRelevantMechanic(Mechanic<?> mechanic) {
        Block up = this.loc.getBlock().getRelative(BlockFace.UP); // take mechanic
        Block hopper = this.loc.getBlock().getRelative(BlockFace.DOWN);
        Block target = BlockUtil.getPointingBlock(hopper, false); // put mechanic
        Location mechLoc = mechanic.getLocation();
        return mechLoc.getBlock().equals(up) || mechLoc.getBlock().equals(target);
    }

    @EventHandler
    public void onMechanicRemove(MechanicRemoveEvent event) {
        if (event.getMechanic() == this.takeMechanic || event.getMechanic() == this.putMechanic) {
            Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                if (!event.getMechanic().exists()) {
                    if (event.getMechanic() == this.takeMechanic) {
                        takeMechanic = null;
                    } else if (event.getMechanic() == this.putMechanic) {
                        this.putMechanic = null;
                    }
                }
            });
        }
    }

    @Override
    public void think() {
        if (this.takeMechanic != null && this.putMechanic != null && !this.takeMechanic.isTransferEmpty()) {
            this.putMechanic.pipePut(this.takeMechanic, new PipePutEvent(null, this.takeMechanic, this));
        }
    }
}
