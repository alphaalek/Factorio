package dk.superawesome.factories.util;

import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;

import java.util.Arrays;
import java.util.function.Predicate;

@SuppressWarnings("deprecation")
public interface BlockValidator {

    static BlockValidator fromOut(LazyInit<Array<Material>> modern, int... ids) {
        return from(modern, b -> {
            Material type = b.getItemType();
            return Arrays.stream(ids).anyMatch(i -> type.getId() == i);
        });
    }

    static BlockValidator fromIn(LazyInit<Material> modern, int... ids) {
        return fromOut(LazyInit.of(() -> Array.just(modern.get())), ids);
    }

    static BlockValidator from(LazyInit<Array<Material>> modern, Predicate<MaterialData> legacyFilter) {
        return new BlockValidator() {
            @Override
            public boolean is(Material material, byte legacyData) {
                if (BlockUtil.LEGACY) {
                    return legacyFilter.test(new MaterialData(material, legacyData));
                } else {
                    return modern.get().stream().anyMatch(m -> material == m);
                }
            }

            @Override
            public boolean is(Block block) {
                return is(block.getType(), BlockUtil.LEGACY ? block.getData() : 0);
            }
        };
    }

    boolean is(Material material, byte legacyData);

    boolean is(Block block);
}
