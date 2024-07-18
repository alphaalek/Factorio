package dk.superawesome.factories.mechanics.impl;

import dk.superawesome.factories.gui.impl.PowerCentralGui;
import dk.superawesome.factories.mechanics.*;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;

public class PowerCentral extends AbstractMechanic<PowerCentral, PowerCentralGui> implements ThinkingMechanic {

    private final ThinkDelayHandler thinkDelayHandler = new ThinkDelayHandler(20);

    private double energy;
    private boolean lit;

    public PowerCentral(Location loc, BlockFace rotation) {
        super(loc, rotation);
    }

    @Override
    public MechanicProfile<PowerCentral, PowerCentralGui> getProfile() {
        return Profiles.POWER_CENTRAL;
    }

    @Override
    public void think() {

        if (energy > 0 && !lit) {
            lit = true;
            updateLight();
        }

        if (energy == 0 && lit) {
            lit = false;
            updateLight();
        }
    }

    private void updateLight() {
        if (loc.getWorld() != null) {
            Block block = loc.getWorld().getBlockAt(BlockUtil.getRel(loc, getProfile().getBuilding().getRelatives().get(1)));
            BlockData data = block.getBlockData();
            if (data instanceof Lightable) {
                ((Lightable)data).setLit(lit);
                block.setBlockData(data);
            }
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
