package dk.superawesome.factories.listeners;

import dk.superawesome.factories.Factories;
import dk.superawesome.factories.production.ProductionType;
import dk.superawesome.factories.production.ProductionTypes;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.Optional;

public class ChunkLoadListener implements Listener {

    @EventHandler

    public void onChunkLoad(ChunkLoadEvent event) {
        for (BlockState state : event.getChunk().getTileEntities()) {
            if (state instanceof Sign) {
                Sign sign = (Sign) state;
                if (!sign.getLine(0).startsWith("[")
                        || !sign.getLine(1).startsWith("]")) {
                    continue;
                }

                String type = sign.getLine(0).substring(1, sign.getLine(0).length() - 1);
                Optional<ProductionType> productionTypeOptional = ProductionTypes.getProductions()
                        .stream()
                        .filter(b -> b.getName().equals(type))
                        .findFirst();
                if (!productionTypeOptional.isPresent()) {
                    continue;
                }

                Factories.get().getProductionHandler().load(productionTypeOptional.get(), state.getLocation());
            }
        }
    }
}
