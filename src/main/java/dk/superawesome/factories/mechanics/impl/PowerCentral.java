package dk.superawesome.factories.mechanics.impl;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.gui.impl.PowerCentralGui;
import dk.superawesome.factories.mechanics.*;
import dk.superawesome.factories.mechanics.routes.Routes;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Switch;

public class PowerCentral extends AbstractMechanic<PowerCentral, PowerCentralGui> implements ThinkingMechanic {

    private final ThinkDelayHandler thinkDelayHandler = new ThinkDelayHandler(20);

    private Block lever;
    private double energy;
    private boolean turnedOn;

    public PowerCentral(Location loc, BlockFace rotation) {
        super(loc, rotation);
    }

    @Override
    public void blocksLoaded() {
        lever = getLocation().getBlock().getRelative(getRotation());
        if (lever.getType() != Material.LEVER) {
            // invalid power central
            Factories.get().getMechanicManager(getLocation().getWorld()).unload(this);
        }
    }

    @Override
    public MechanicProfile<PowerCentral, PowerCentralGui> getProfile() {
        return Profiles.POWER_CENTRAL;
    }

    @Override
    public void think() {
        double prev = energy;
        if (energy > 0) {
            Routes.startSignalRoute(lever, this);

            // TODO looks like calling both? (repeater signals)
            if (!turnedOn && energy < prev) {
                turnedOn = true;
                update();
            }
        }

        if ((energy == 0 || energy == prev) && turnedOn) {
            turnedOn = false;
            update();
        }
    }

    private void update() {
        if (loc.getWorld() != null) {
            Block block = loc.getWorld().getBlockAt(BlockUtil.getRel(loc, getProfile().getBuilding().getRelatives().get(1)));
            BlockData data = block.getBlockData();
            if (data instanceof Lightable) {
                ((Lightable)data).setLit(turnedOn);
                block.setBlockData(data);
            }

            Switch lever = (Switch) this.lever.getBlockData();
            lever.setPowered(turnedOn);
            this.lever.setBlockData(lever);
        }
    }

    @Override
    public ThinkDelayHandler getDelayHandler() {
        return thinkDelayHandler;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }
}
