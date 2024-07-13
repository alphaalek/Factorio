package dk.superawesome.factories.building;

import dk.superawesome.factories.util.Identifiable;
import org.bukkit.Location;
import org.bukkit.util.BlockVector;

import java.util.List;
import java.util.function.Consumer;

public interface Building extends Identifiable {

    List<Consumer<Location>> getBlocks();

    List<BlockVector> getRelatives();
}
