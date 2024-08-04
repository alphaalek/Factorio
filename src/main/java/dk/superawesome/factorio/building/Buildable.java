package dk.superawesome.factorio.building;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.List;
import java.util.function.BiConsumer;

public interface Buildable {

    List<BiConsumer<Block, BlockFace>> getBlocks();
}
