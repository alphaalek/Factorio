package dk.superawesome.factorio.building.impl;

import dk.superawesome.factorio.building.Buildable;
import dk.superawesome.factorio.building.Building;
import dk.superawesome.factorio.mechanics.Profiles;
import dk.superawesome.factorio.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.util.BlockVector;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public class GeneratorBuilding implements Building, Buildable {

    private final List<BlockVector> relatives = Arrays.asList(
            ORIGIN,
            new BlockVector(0, 1, 0),
            new BlockVector(0, 2, 0),
            WALL_SIGN,
            new BlockVector(1, 0, 0)
    );

    private final List<BiConsumer<Block, BlockFace>> blocks = Arrays.asList(
            (b, r) -> b.setType(Material.BRICKS),
            (b, r) -> b.setType(Material.SMOKER),
            (b, r) -> b.setType(Material.CAMPFIRE),
            (b, r) -> {},
            (b, r) -> setLever(b, BlockUtil.getRotationRelative(DEFAULT_ROTATION, DEFAULT_ROTATION.getOppositeFace(), r))
    );

    private void setLever(Block block, BlockFace rotation) {
        block.setType(Material.LEVER, false); // do not apply physics

        Switch lever = (Switch) block.getBlockData();
        lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
        lever.setFacing(rotation);

        block.setBlockData(lever);
    }

    @Override
    public List<BiConsumer<Block, BlockFace>> getBlocks() {
        return blocks;
    }

    @Override
    public List<BlockVector> getRelatives() {
        return relatives;
    }

    @Override
    public int getID() {
        return Profiles.GENERATOR.getID();
    }
}
