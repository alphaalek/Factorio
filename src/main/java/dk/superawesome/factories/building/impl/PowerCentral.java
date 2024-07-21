package dk.superawesome.factories.building.impl;

import dk.superawesome.factories.building.Building;
import dk.superawesome.factories.mechanics.Profiles;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.sign.Side;
import org.bukkit.util.BlockVector;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

public class PowerCentral implements Building {

    private final List<BlockVector> relatives = Arrays.asList(
            new BlockVector(),
            new BlockVector(0, 1, 0),
            new BlockVector(0, 2, 0),
            new BlockVector(-1, 1, 0),
            new BlockVector(-1, 0, 0)
    );

    private final List<BiConsumer<Block, BlockFace>> blocks = Arrays.asList(
            (b, r) -> b.setType(Material.NETHER_QUARTZ_ORE),
            (b, r) -> b.setType(Material.REDSTONE_LAMP),
            (b, r) -> b.setType(Material.REDSTONE_TORCH),
            (b, r) -> {
                // get the sign (the block below) by using some rotation magic
                Block sign = BlockUtil.getRel(
                                getOri(b.getLocation(), relatives.get(3), r), BlockUtil.rotateVec(relatives.get(4), BlockFace.WEST, r)
                ).getBlock();

                // apply same data, simulating a copy
                b.setType(sign.getType());
                b.setBlockData(sign.getBlockData());

                Sign signState = (Sign) sign.getState();
                Sign newSignState = (Sign) b.getState();
                int i = 0;
                for (String line : signState.getSide(Side.FRONT).getLines()) {
                    newSignState.getSide(Side.FRONT).setLine(i++, line);
                }
                newSignState.update();
            },
            (b, r) -> {
                b.setType(Material.LEVER, false); // do not apply physics

                Switch lever = (Switch) b.getBlockData();
                lever.setAttachedFace(FaceAttachable.AttachedFace.WALL);
                lever.setFacing(r);

                b.setBlockData(lever);
            }
    );

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
        return Profiles.POWER_CENTRAL.getID();
    }
}
