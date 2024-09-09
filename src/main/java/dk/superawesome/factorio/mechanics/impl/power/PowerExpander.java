package dk.superawesome.factorio.mechanics.impl.power;

import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.util.Action;
import dk.superawesome.factorio.util.ChainRunnable;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class PowerExpander extends AbstractMechanic<PowerExpander> implements SignalInvoker {

    private boolean invoked;

    public PowerExpander(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign) {
        super(loc, rotation, context, hasWallSign);
    }

    @Override
    public MechanicProfile<PowerExpander> getProfile() {
        return Profiles.POWER_EXPANDER;
    }

    @Override
    public boolean invoke(SignalSource source) {
        if (invoked) {
            return false;
        }

        if (source instanceof PowerCentral pc) {
            AtomicBoolean transferred = new AtomicBoolean();
            invoked = true;

            BlockUtil.forRelative(this.loc.getBlock(), new Action<>() {

                Block invoker = null;
                ChainRunnable invoke = ChainRunnable.empty();

                @Override
                public void accept(Block block) {
                    if (block.getType() == Material.STICKY_PISTON) {
                        Block point = BlockUtil.getPointingBlock(block, false);
                        if (!point.equals(PowerExpander.this.loc.getBlock())) {
                            invoke = invoke.thenDo(() -> {
                                if (Routes.invokePCOutput(point, point.getLocation(), Arrays.asList(point, invoker), pc)) {
                                    transferred.set(true);
                                }
                            });
                        } else {
                            invoker = block;
                        }
                    }
                }

                @Override
                public void onFinish() {
                    invoke.run();
                }
            });

            invoked = false;
            return transferred.get();
        }

        return false;
    }
}
