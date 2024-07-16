package dk.superawesome.factories.listeners;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.mehcanics.MechanicProfile;
import dk.superawesome.factories.mehcanics.Profiles;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.Arrays;
import java.util.Optional;

public class ChunkLoadListener implements Listener {

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // TODO loader nogen gange ikke når man pender join ved server boot
        World world = event.getChunk().getWorld();
        for (BlockState state : event.getChunk().getTileEntities()) {
            if (state instanceof Sign) { // TODO: kun wall signs?
                // check if this sign is related to a mechanic
                Sign sign = (Sign) state;
                if (!sign.getLine(0).startsWith("[")
                        || !sign.getLine(0).endsWith("]")) {
                    continue;
                }
                String type = sign.getLine(0)
                        .replaceAll("]", "")
                        .replaceAll("\\[", "");

                Optional<MechanicProfile<?, ?>> mechanicProfile = Profiles.getProfiles()
                        .stream()
                        .filter(b -> b.getName().equals(type))
                        .findFirst();
                if (!mechanicProfile.isPresent()) {
                    continue;
                }

                // get the block which the sign is hanging on, because this block is the root of the mechanic
                Block on = BlockUtil.getPointingBlock(state.getBlock(), true);
                if (on == null) {
                    continue;
                }

                // TODO doesn't always load

                // load this mechanic
                Factories.get().getMechanicManager(world).load(mechanicProfile.get(), on.getLocation());
            }
        }
    }
}
