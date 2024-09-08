package dk.superawesome.factorio.mechanics.impl.power;

import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PowerExpander extends AbstractMechanic<PowerExpander> implements SignalInvoker {

    private final List<Block> rel = new ArrayList<>();

    public PowerExpander(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        rel.clear();
        for (BlockFace dir : Routes.SIGNAL_EXPAND_DIRECTIONS) {
            rel.add(loc.getBlock().getRelative(dir));
        }
    }

    @Override
    public MechanicProfile<PowerExpander> getProfile() {
        return Profiles.POWER_EXPANDER;
    }

    @Override
    public boolean invoke(SignalSource source) {
        if (source instanceof PowerCentral pc) {
            boolean transferred = false;
            for (Block rel : this.rel) {
                if (rel.getType() == Material.STICKY_PISTON) {
                    Block point = BlockUtil.getPointingBlock(rel, false);
                    if (Routes.invokePCOutput(point, point.getLocation(), rel, pc)) {
                        transferred = true;
                    }
                }
            }
            return transferred;
        }
        return false;
    }
}