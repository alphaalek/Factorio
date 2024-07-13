package dk.superawesome.factories.util.mappings;

import dk.superawesome.factories.util.LazyInit;
import dk.superawesome.factories.util.statics.BlockUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.material.MaterialData;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("deprecation")
public class ItemMappings {

    private static final Map<String, ItemSupplier> suppliers = new HashMap<>();
    private static final Map<Material, MappingData> dataByMaterialModern = new HashMap<>();
    private static final Map<MaterialData, MappingData> dataByMaterialLegacy = new HashMap<>();
    private static final Function<Material, ItemSupplier> DEFAULT = m -> getFrom(LazyInit.of(() -> m), (short) 0);

    private static void loadMapping(MappingData data) {
        suppliers.put(data.identifier, data.get());

        if (BlockUtil.LEGACY) {
            ItemStack item = data.supplier.generateItem();
            dataByMaterialLegacy.put(item.getData(), data);
        } else {
            dataByMaterialModern.put(data.get().getMaterial(), data);
        }
    }

    public static ItemSupplier get(String identifier) {
        return suppliers.get(identifier);
    }

    public static ItemSupplier getOrDefault(String identifier) {
        return has(identifier) ? get(identifier) : DEFAULT.apply(Material.valueOf(identifier.toUpperCase()));
    }

    public static String getIdentifier(String identifier) {
        return has(identifier) ? identifier : getFromOrDefault(identifier).getIdentifier();
    }

    public static boolean has(String identifier) {
        return suppliers.containsKey(identifier);
    }

    public static MappingData getFromModern(Material material) {
        if (BlockUtil.LEGACY) {
            return dataByMaterialModern.get(material);
        }
        throw new UnsupportedOperationException();
    }

    public static MappingData getFromLegacy(Material material, short data) {
        if (BlockUtil.LEGACY) {
            return dataByMaterialLegacy.get(new MaterialData(material, (byte) data));
        }
        throw new UnsupportedOperationException();
    }

    public static MappingData getFromOrDefaultModern(Material material) {
        if (!BlockUtil.LEGACY) {
            return dataByMaterialModern.getOrDefault(material,
                    new MappingData(material.toString().toLowerCase(), getFrom(LazyInit.of(() -> material), (short) 0)));
        }
        throw new UnsupportedOperationException();
    }

    public static MappingData getFromOrDefaultLegacy(Material material, short data) {
        if (BlockUtil.LEGACY) {
            return getFromOrDefaultLegacy(material, new MaterialData(material, (byte) data));
        }
        throw new UnsupportedOperationException();
    }

    public static MappingData getFromOrDefaultLegacy(Material material, MaterialData data) {
        if (BlockUtil.LEGACY) {
            return dataByMaterialLegacy.getOrDefault(data,
                    new MappingData(material.toString().toLowerCase(), getFrom(LazyInit.of(() -> material), data.getData())));
        }
        throw new UnsupportedOperationException();
    }

    public static MappingData getFromOrDefaultLegacy(Block block, boolean ignoreData) {
        if (BlockUtil.LEGACY) {
            return getFromOrDefaultLegacy(block.getType(), ignoreData ? (byte) 0 : block.getData());
        }
        throw new UnsupportedOperationException();
    }

    public static MappingData getFromOrDefault(ItemStack item) {
        if (BlockUtil.LEGACY) {
            return getFromOrDefaultLegacy(item.getType(), item.getData());
        } else {
            return getFromOrDefaultModern(item.getType());
        }
    }

    public static MappingData getFromOrDefault(Block block) {
        if (BlockUtil.LEGACY) {
            return getFromOrDefaultLegacy(block, true);
        } else {
            return getFromOrDefaultModern(block.getType());
        }
    }

    public static MappingData getFromOrDefault(String material, short data) {
        try {
            ItemSupplier supplier = getOrDefault(material);
            if (supplier == null) {
                return null;
            }

            Material mat = supplier.getMaterial();
            if (BlockUtil.LEGACY) {
                return getFromOrDefaultLegacy(mat, data);
            } else {
                return getFromOrDefaultModern(mat);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    public static MappingData getFromOrDefault(String material) {
        return getFromOrDefault(material, (short) 0);
    }

    public static boolean hasDataModern(Material material) {
        if (!BlockUtil.LEGACY) {
            return dataByMaterialModern.containsKey(material);
        }
        throw new UnsupportedOperationException();
    }

    public static boolean hasDataLegacy(Material material, short data) {
        if (BlockUtil.LEGACY) {
            return dataByMaterialLegacy.containsKey(new MaterialData(material, (byte) data));
        }
        throw new UnsupportedOperationException();
    }

    private static ItemSupplier getFrom(LazyInit<Material> material, short data) {
        return new ItemSupplier() {
            @Override
            public ItemStack generateItem(int amount, short damage) {
                if (BlockUtil.LEGACY) {
                    return new ItemStack(getMaterial(), amount, damage, (byte) data);
                } else {
                    ItemStack stack = new ItemStack(getMaterial(), amount);
                    if (damage > 0 && stack.getItemMeta() instanceof Damageable) {
                        ((Damageable) stack.getItemMeta()).setDamage(damage);
                    }
                    return stack;
                }
            }

            @Override
            public Material getMaterial() {
                return material.get();
            }
        };
    }

    private static ItemSupplier getLegacy(LazyInit<Material> material, short data) {
        return getFrom(material, data);
    }

    private static ItemSupplier getModern(LazyInit<Material> material) {
        return getFrom(material, (short) 0);
    }

    private static ItemSupplier get(ItemSupplier legacy, ItemSupplier modern) {
        return new ItemSupplier() {
            @Override
            public ItemStack generateItem(int amount, short damage) {
                return BlockUtil.LEGACY ? legacy.generateItem(amount, damage) : modern.generateItem(amount, damage);
            }

            @Override
            public Material getMaterial() {
                return BlockUtil.LEGACY ? legacy.getMaterial() : modern.getMaterial();
            }
        };
    }

    public static void load() {
        // grass, dirt
        loadMapping(Builder.of("coarse_dirt")
                .modern(LazyInit.of(() -> Material.COARSE_DIRT))
                .legacy(Material.DIRT, (short) 1).get());
        loadMapping(Builder.of("podzol")
                .modern(LazyInit.of(() -> Material.PODZOL))
                .legacy(Material.DIRT, (short) 2).get());
        loadMapping(Builder.of("red_sand")
                .modern(LazyInit.of(() -> Material.RED_SAND))
                .legacy(Material.SAND, (short) 1).get());
        loadMapping(Builder.of("grass")
                .modern(LazyInit.of(() -> Material.GRASS_BLOCK))
                .legacy("grass").get());

        // stones
        loadMapping(Builder.of("granite")
                .modern(LazyInit.of(() -> Material.GRANITE))
                .legacy(Material.STONE, (short) 1).get());
        loadMapping(Builder.of("polished_granite")
                .modern(LazyInit.of(() -> Material.POLISHED_GRANITE))
                .legacy(Material.STONE, (short) 2).get());
        loadMapping(Builder.of("diorite")
                .modern(LazyInit.of(() -> Material.DIORITE))
                .legacy(Material.STONE, (short) 3).get());
        loadMapping(Builder.of("polished_diorite")
                .modern(LazyInit.of(() -> Material.POLISHED_DIORITE))
                .legacy(Material.STONE, (short) 4).get());
        loadMapping(Builder.of("andesite")
                .modern(LazyInit.of(() -> Material.ANDESITE))
                .legacy(Material.STONE, (short) 5).get());
        loadMapping(Builder.of("polished_andesite")
                .modern(LazyInit.of(() -> Material.POLISHED_ANDESITE))
                .legacy(Material.STONE, (short) 6).get());

        // huge mushrooms
        loadMapping(Builder.of("brown_mushroom_block")
                .modern(LazyInit.of(() -> Material.BROWN_MUSHROOM_BLOCK))
                .legacy("huge_mushroom_1").get());
        loadMapping(Builder.of("red_mushroom_block")
                .modern(LazyInit.of(() -> Material.RED_MUSHROOM_BLOCK))
                .legacy("huge_mushroom_2").get());

        // planks
        loadMapping(Builder.of("oak_planks")
                .modern(LazyInit.of(() -> Material.OAK_PLANKS))
                .legacy("wood").get());
        loadMapping(Builder.of("spruce_planks")
                .modern(LazyInit.of(() -> Material.SPRUCE_PLANKS))
                .legacy("wood", (short) 1).get());
        loadMapping(Builder.of("birch_planks")
                .modern(LazyInit.of(() -> Material.BIRCH_PLANKS))
                .legacy("wood", (short) 2).get());
        loadMapping(Builder.of("jungle_planks")
                .modern(LazyInit.of(() -> Material.JUNGLE_PLANKS))
                .legacy("wood", (short) 3).get());
        loadMapping(Builder.of("acacia_planks")
                .modern(LazyInit.of(() -> Material.ACACIA_PLANKS))
                .legacy("wood", (short) 4).get());
        loadMapping(Builder.of("dark_oak_planks")
                .modern(LazyInit.of(() -> Material.DARK_OAK_PLANKS))
                .legacy("wood", (short) 5).get());

        // saplings
        loadMapping(Builder.of("oak_sapling")
                .modern(LazyInit.of(() -> Material.OAK_SAPLING))
                .legacy("sapling").get());
        loadMapping(Builder.of("spruce_sapling")
                .modern(LazyInit.of(() -> Material.SPRUCE_SAPLING))
                .legacy("sapling", (short) 1).get());
        loadMapping(Builder.of("birch_sapling")
                .modern(LazyInit.of(() -> Material.BIRCH_SAPLING))
                .legacy("sapling", (short) 2).get());
        loadMapping(Builder.of("jungle_sapling")
                .modern(LazyInit.of(() -> Material.JUNGLE_SAPLING))
                .legacy("sapling", (short) 3).get());
        loadMapping(Builder.of("acacia_sapling")
                .modern(LazyInit.of(() -> Material.ACACIA_SAPLING))
                .legacy("sapling", (short) 4).get());
        loadMapping(Builder.of("dark_oak_sapling")
                .modern(LazyInit.of(() -> Material.DARK_OAK_SAPLING))
                .legacy("sapling", (short) 5).get());

        // logs
        loadMapping(Builder.of("oak_log")
                .modern(LazyInit.of(() -> Material.OAK_LOG))
                .legacy("log").get());
        loadMapping(Builder.of("spruce_log")
                .modern(LazyInit.of(() -> Material.SPRUCE_LOG))
                .legacy("log", (short) 1).get());
        loadMapping(Builder.of("birch_log")
                .modern(LazyInit.of(() -> Material.BIRCH_LOG))
                .legacy("log", (short) 2).get());
        loadMapping(Builder.of("jungle_log")
                .modern(LazyInit.of(() -> Material.JUNGLE_LOG))
                .legacy("log", (short) 3).get());
        loadMapping(Builder.of("acacia_log")
                .modern(LazyInit.of(() -> Material.ACACIA_LOG))
                .legacy("log_2").get());
        loadMapping(Builder.of("dark_oak_log")
                .modern(LazyInit.of(() -> Material.DARK_OAK_LOG))
                .legacy("log_2", (short) 1).get());

        // leaves
        loadMapping(Builder.of("oak_leaves")
                .modern(LazyInit.of(() -> Material.OAK_LEAVES))
                .legacy("leaves").get());
        loadMapping(Builder.of("spruce_leaves")
                .modern(LazyInit.of(() -> Material.SPRUCE_LEAVES))
                .legacy("leaves", (short) 1).get());
        loadMapping(Builder.of("birch_leaves")
                .modern(LazyInit.of(() -> Material.BIRCH_LEAVES))
                .legacy("leaves", (short) 2).get());
        loadMapping(Builder.of("jungle_leaves")
                .modern(LazyInit.of(() -> Material.JUNGLE_LEAVES))
                .legacy("leaves", (short) 3).get());
        loadMapping(Builder.of("acacia_leaves")
                .modern(LazyInit.of(() -> Material.ACACIA_LEAVES))
                .legacy("leaves_2").get());
        loadMapping(Builder.of("dark_oak_leaves")
                .modern(LazyInit.of(() -> Material.DARK_OAK_LEAVES))
                .legacy("leaves_2", (short) 1).get());

        // doors
        loadMapping(Builder.of("oak_door")
                .modern(LazyInit.of(() -> Material.OAK_DOOR))
                .legacy("wood_door").get());
        loadMapping(Builder.of("spruce_door")
                .modern(LazyInit.of(() -> Material.SPRUCE_DOOR))
                .legacy("spruce_door_item").get());
        loadMapping(Builder.of("birch_door")
                .modern(LazyInit.of(() -> Material.BIRCH_DOOR))
                .legacy("birch_door_item").get());
        loadMapping(Builder.of("jungle_door")
                .modern(LazyInit.of(() -> Material.JUNGLE_DOOR))
                .legacy("jungle_door_item").get());
        loadMapping(Builder.of("acacia_door")
                .modern(LazyInit.of(() -> Material.ACACIA_DOOR))
                .legacy("acacia_door_item").get());
        loadMapping(Builder.of("dark_oak_door")
                .modern(LazyInit.of(() -> Material.DARK_OAK_DOOR))
                .legacy("dark_oak_door_item").get());

        // banners
        loadMapping(Builder.of("black_banner")
                .modern(LazyInit.of(() -> Material.BLACK_BANNER))
                .legacy("banner").get());
        loadMapping(Builder.of("red_banner")
                .modern(LazyInit.of(() -> Material.RED_BANNER))
                .legacy("banner", (short) 1).get());
        loadMapping(Builder.of("green_banner")
                .modern(LazyInit.of(() -> Material.GREEN_BANNER))
                .legacy("banner", (short) 2).get());
        loadMapping(Builder.of("brown_banner")
                .modern(LazyInit.of(() -> Material.BROWN_BANNER))
                .legacy("banner", (short) 3).get());
        loadMapping(Builder.of("blue_banner")
                .modern(LazyInit.of(() -> Material.BLUE_BANNER))
                .legacy("banner", (short) 4).get());
        loadMapping(Builder.of("purple_banner")
                .modern(LazyInit.of(() -> Material.PURPLE_BANNER))
                .legacy("banner", (short) 5).get());
        loadMapping(Builder.of("cyan_banner")
                .modern(LazyInit.of(() -> Material.CYAN_BANNER))
                .legacy("banner", (short) 6).get());
        loadMapping(Builder.of("light_gray_banner")
                .modern(LazyInit.of(() -> Material.LIGHT_GRAY_BANNER))
                .legacy("banner", (short) 7).get());
        loadMapping(Builder.of("gray_banner")
                .modern(LazyInit.of(() -> Material.GRAY_BANNER))
                .legacy("banner", (short) 8).get());
        loadMapping(Builder.of("pink_banner")
                .modern(LazyInit.of(() -> Material.PINK_BANNER))
                .legacy("banner", (short) 9).get());
        loadMapping(Builder.of("lime_banner")
                .modern(LazyInit.of(() -> Material.LIME_BANNER))
                .legacy("banner", (short) 10).get());
        loadMapping(Builder.of("yellow_banner")
                .modern(LazyInit.of(() -> Material.YELLOW_BANNER))
                .legacy("banner", (short) 11).get());
        loadMapping(Builder.of("light_blue_banner")
                .modern(LazyInit.of(() -> Material.LIGHT_BLUE_BANNER))
                .legacy("banner", (short) 12).get());
        loadMapping(Builder.of("magenta_banner")
                .modern(LazyInit.of(() -> Material.MAGENTA_BANNER))
                .legacy("banner", (short) 13).get());
        loadMapping(Builder.of("orange_banner")
                .modern(LazyInit.of(() -> Material.ORANGE_BANNER))
                .legacy("banner", (short) 14).get());
        loadMapping(Builder.of("white_banner")
                .modern(LazyInit.of(() -> Material.WHITE_BANNER))
                .legacy("banner", (short) 15).get());

        // wool
        loadMapping(Builder.of("white_wool")
                .modern(LazyInit.of(() -> Material.WHITE_WOOL))
                .legacy("wool").get());
        loadMapping(Builder.of("orange_wool")
                .modern(LazyInit.of(() -> Material.ORANGE_WOOL))
                .legacy("wool", (short) 1).get());
        loadMapping(Builder.of("magenta_wool")
                .modern(LazyInit.of(() -> Material.MAGENTA_WOOL))
                .legacy("wool", (short) 2).get());
        loadMapping(Builder.of("light_blue_wool")
                .modern(LazyInit.of(() -> Material.LIGHT_BLUE_WOOL))
                .legacy("wool", (short) 3).get());
        loadMapping(Builder.of("yellow_wool")
                .modern(LazyInit.of(() -> Material.YELLOW_WOOL))
                .legacy("wool", (short) 4).get());
        loadMapping(Builder.of("lime_wool")
                .modern(LazyInit.of(() -> Material.LIME_WOOL))
                .legacy("wool", (short) 5).get());
        loadMapping(Builder.of("pink_wool")
                .modern(LazyInit.of(() -> Material.PINK_WOOL))
                .legacy("wool", (short) 6).get());
        loadMapping(Builder.of("gray_wool")
                .modern(LazyInit.of(() -> Material.GRAY_WOOL))
                .legacy("wool", (short) 7).get());
        loadMapping(Builder.of("light_gray_wool")
                .modern(LazyInit.of(() -> Material.LIGHT_GRAY_WOOL))
                .legacy("wool", (short) 8).get());
        loadMapping(Builder.of("cyan_wool")
                .modern(LazyInit.of(() -> Material.CYAN_WOOL))
                .legacy("wool", (short) 9).get());
        loadMapping(Builder.of("purple_wool")
                .modern(LazyInit.of(() -> Material.PURPLE_WOOL))
                .legacy("wool", (short) 10).get());
        loadMapping(Builder.of("blue_wool")
                .modern(LazyInit.of(() -> Material.BLUE_WOOL))
                .legacy("wool", (short) 11).get());
        loadMapping(Builder.of("brown_wool")
                .modern(LazyInit.of(() -> Material.BROWN_WOOL))
                .legacy("wool", (short) 12).get());
        loadMapping(Builder.of("green_wool")
                .modern(LazyInit.of(() -> Material.GREEN_WOOL))
                .legacy("wool", (short) 13).get());
        loadMapping(Builder.of("red_wool")
                .modern(LazyInit.of(() -> Material.RED_WOOL))
                .legacy("wool", (short) 14).get());
        loadMapping(Builder.of("black_wool")
                .modern(LazyInit.of(() -> Material.BLACK_WOOL))
                .legacy("wool", (short) 15).get());
        loadMapping(Builder.of("oak_fence")
                .modern(LazyInit.of(() -> Material.OAK_FENCE))
                .legacy("fence").get());

        // carpets
        loadMapping(Builder.of("white_carpet")
                .modern(LazyInit.of(() -> Material.WHITE_CARPET))
                .legacy("carpet").get());
        loadMapping(Builder.of("orange_carpet")
                .modern(LazyInit.of(() -> Material.ORANGE_CARPET))
                .legacy("carpet", (short) 1).get());
        loadMapping(Builder.of("magenta_carpet")
                .modern(LazyInit.of(() -> Material.MAGENTA_CARPET))
                .legacy("carpet", (short) 2).get());
        loadMapping(Builder.of("light_blue_carpet")
                .modern(LazyInit.of(() -> Material.LIGHT_BLUE_CARPET))
                .legacy("carpet", (short) 3).get());
        loadMapping(Builder.of("yellow_carpet")
                .modern(LazyInit.of(() -> Material.YELLOW_CARPET))
                .legacy("carpet", (short) 4).get());
        loadMapping(Builder.of("lime_carpet")
                .modern(LazyInit.of(() -> Material.LIME_CARPET))
                .legacy("carpet", (short) 5).get());
        loadMapping(Builder.of("pink_carpet")
                .modern(LazyInit.of(() -> Material.PINK_CARPET))
                .legacy("carpet", (short) 6).get());
        loadMapping(Builder.of("gray_carpet")
                .modern(LazyInit.of(() -> Material.GRAY_CARPET))
                .legacy("carpet", (short) 7).get());
        loadMapping(Builder.of("light_gray_carpet")
                .modern(LazyInit.of(() -> Material.LIGHT_GRAY_CARPET))
                .legacy("carpet", (short) 8).get());
        loadMapping(Builder.of("cyan_carpet")
                .modern(LazyInit.of(() -> Material.CYAN_CARPET))
                .legacy("carpet", (short) 9).get());
        loadMapping(Builder.of("purple_carpet")
                .modern(LazyInit.of(() -> Material.PURPLE_CARPET))
                .legacy("carpet", (short) 10).get());
        loadMapping(Builder.of("blue_carpet")
                .modern(LazyInit.of(() -> Material.BLUE_CARPET))
                .legacy("carpet", (short) 11).get());
        loadMapping(Builder.of("brown_carpet")
                .modern(LazyInit.of(() -> Material.BROWN_CARPET))
                .legacy("carpet", (short) 12).get());
        loadMapping(Builder.of("green_carpet")
                .modern(LazyInit.of(() -> Material.GREEN_CARPET))
                .legacy("carpet", (short) 13).get());
        loadMapping(Builder.of("red_carpet")
                .modern(LazyInit.of(() -> Material.RED_CARPET))
                .legacy("carpet", (short) 14).get());
        loadMapping(Builder.of("black_carpet")
                .modern(LazyInit.of(() -> Material.BLACK_CARPET))
                .legacy("carpet", (short) 15).get());


        // stained glass pane
        loadMapping(Builder.of("white_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.WHITE_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane").get());
        loadMapping(Builder.of("orange_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.ORANGE_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 1).get());
        loadMapping(Builder.of("magenta_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.MAGENTA_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 2).get());
        loadMapping(Builder.of("light_blue_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.LIGHT_BLUE_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 3).get());
        loadMapping(Builder.of("yellow_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.YELLOW_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 4).get());
        loadMapping(Builder.of("lime_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.LIME_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 5).get());
        loadMapping(Builder.of("pink_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.PINK_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 6).get());
        loadMapping(Builder.of("gray_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.GRAY_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 7).get());
        loadMapping(Builder.of("light_gray_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.LIGHT_GRAY_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 8).get());
        loadMapping(Builder.of("cyan_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.CYAN_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 9).get());
        loadMapping(Builder.of("purple_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.PURPLE_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 10).get());
        loadMapping(Builder.of("blue_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.BLUE_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 11).get());
        loadMapping(Builder.of("brown_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.BROWN_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 12).get());
        loadMapping(Builder.of("green_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.GREEN_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 13).get());
        loadMapping(Builder.of("red_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.RED_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 14).get());
        loadMapping(Builder.of("black_stained_glass_pane")
                .modern(LazyInit.of(() -> Material.BLACK_STAINED_GLASS_PANE))
                .legacy("stained_glass_pane", (short) 15).get());

        // stained glass
        loadMapping(Builder.of("white_stained_glass")
                .modern(LazyInit.of(() -> Material.WHITE_STAINED_GLASS))
                .legacy("stained_glass").get());
        loadMapping(Builder.of("orange_stained_glass")
                .modern(LazyInit.of(() -> Material.ORANGE_STAINED_GLASS))
                .legacy("stained_glass", (short) 1).get());
        loadMapping(Builder.of("magenta_stained_glass")
                .modern(LazyInit.of(() -> Material.MAGENTA_STAINED_GLASS))
                .legacy("stained_glass", (short) 2).get());
        loadMapping(Builder.of("light_blue_stained_glass")
                .modern(LazyInit.of(() -> Material.LIGHT_BLUE_STAINED_GLASS))
                .legacy("stained_glass", (short) 3).get());
        loadMapping(Builder.of("yellow_stained_glass")
                .modern(LazyInit.of(() -> Material.YELLOW_STAINED_GLASS))
                .legacy("stained_glass", (short) 4).get());
        loadMapping(Builder.of("lime_stained_glass")
                .modern(LazyInit.of(() -> Material.LIME_STAINED_GLASS))
                .legacy("stained_glass", (short) 5).get());
        loadMapping(Builder.of("pink_stained_glass")
                .modern(LazyInit.of(() -> Material.PINK_STAINED_GLASS))
                .legacy("stained_glass", (short) 6).get());
        loadMapping(Builder.of("gray_stained_glass")
                .modern(LazyInit.of(() -> Material.GRAY_STAINED_GLASS))
                .legacy("stained_glass", (short) 7).get());
        loadMapping(Builder.of("light_gray_stained_glass")
                .modern(LazyInit.of(() -> Material.LIGHT_GRAY_STAINED_GLASS))
                .legacy("stained_glass", (short) 8).get());
        loadMapping(Builder.of("cyan_stained_glass")
                .modern(LazyInit.of(() -> Material.CYAN_STAINED_GLASS))
                .legacy("stained_glass", (short) 9).get());
        loadMapping(Builder.of("purple_stained_glass")
                .modern(LazyInit.of(() -> Material.PURPLE_STAINED_GLASS))
                .legacy("stained_glass", (short) 10).get());
        loadMapping(Builder.of("blue_stained_glass")
                .modern(LazyInit.of(() -> Material.BLUE_STAINED_GLASS))
                .legacy("stained_glass", (short) 11).get());
        loadMapping(Builder.of("brown_stained_glass")
                .modern(LazyInit.of(() -> Material.BROWN_STAINED_GLASS))
                .legacy("stained_glass", (short) 12).get());
        loadMapping(Builder.of("green_stained_glass")
                .modern(LazyInit.of(() -> Material.GREEN_STAINED_GLASS))
                .legacy("stained_glass", (short) 13).get());
        loadMapping(Builder.of("red_stained_glass")
                .modern(LazyInit.of(() -> Material.RED_STAINED_GLASS))
                .legacy("stained_glass", (short) 14).get());
        loadMapping(Builder.of("black_stained_glass")
                .modern(LazyInit.of(() -> Material.BLACK_STAINED_GLASS))
                .legacy("stained_glass", (short) 15).get());

        // plants
        loadMapping(Builder.of("sunflower")
                .modern(LazyInit.of(() -> Material.SUNFLOWER))
                .legacy("double_plant").get());
        loadMapping(Builder.of("lilac")
                .modern(LazyInit.of(() -> Material.LILAC))
                .legacy("double_plant", (short) 1).get());
        loadMapping(Builder.of("long_grass")
                .modern(LazyInit.of(() -> Material.TALL_GRASS))
                .legacy("long_grass", (short) 1).get());
        loadMapping(Builder.of("tall_grass")
                .modern(LazyInit.of(() -> Material.TALL_GRASS))
                .legacy("double_plant", (short) 2).get());
        loadMapping(Builder.of("large_fern")
                .modern(LazyInit.of(() -> Material.LARGE_FERN))
                .legacy("double_plant", (short) 3).get());
        loadMapping(Builder.of("rose_bush")
                .modern(LazyInit.of(() -> Material.ROSE_BUSH))
                .legacy("double_plant", (short) 4).get());
        loadMapping(Builder.of("peony")
                .modern(LazyInit.of(() -> Material.PEONY))
                .legacy("double_plant", (short) 5).get());
        loadMapping(Builder.of("fern")
                .modern(LazyInit.of(() -> Material.FERN))
                .legacy("long_grass", (short) 2).get());
        loadMapping(Builder.of("dandelion")
                .modern(LazyInit.of(() -> Material.DANDELION))
                .legacy("yellow_flower").get());
        loadMapping(Builder.of("poppy")
                .modern(LazyInit.of(() -> Material.POPPY))
                .legacy("red_rose").get());
        loadMapping(Builder.of("blue_orchid")
                .modern(LazyInit.of(() -> Material.BLUE_ORCHID))
                .legacy("red_rose", (short) 1).get());
        loadMapping(Builder.of("allium")
                .modern(LazyInit.of(() -> Material.ALLIUM))
                .legacy("red_rose", (short) 2).get());
        loadMapping(Builder.of("azure_bluet")
                .modern(LazyInit.of(() -> Material.AZURE_BLUET))
                .legacy("red_rose", (short) 3).get());
        loadMapping(Builder.of("red_tulip")
                .modern(LazyInit.of(() -> Material.RED_TULIP))
                .legacy("red_rose", (short) 4).get());
        loadMapping(Builder.of("orange_tulip")
                .modern(LazyInit.of(() -> Material.ORANGE_TULIP))
                .legacy("red_rose", (short) 5).get());
        loadMapping(Builder.of("white_tulip")
                .modern(LazyInit.of(() -> Material.WHITE_TULIP))
                .legacy("red_rose", (short) 6).get());
        loadMapping(Builder.of("pink_tulip")
                .modern(LazyInit.of(() -> Material.PINK_TULIP))
                .legacy("red_rose", (short) 7).get());
        loadMapping(Builder.of("oxeye_daisy")
                .modern(LazyInit.of(() -> Material.OXEYE_DAISY))
                .legacy("red_rose", (short) 8).get());

        // terracotta
        loadMapping(Builder.of("terracotta")
                .modern(LazyInit.of(() -> Material.TERRACOTTA))
                .legacy("hard_clay").get());
        loadMapping(Builder.of("white_terracotta")
                .modern(LazyInit.of(() -> Material.WHITE_TERRACOTTA))
                .legacy("stained_clay").get());
        loadMapping(Builder.of("orange_terracotta")
                .modern(LazyInit.of(() -> Material.ORANGE_TERRACOTTA))
                .legacy("stained_clay", (short) 1).get());
        loadMapping(Builder.of("magenta_terracotta")
                .modern(LazyInit.of(() -> Material.MAGENTA_TERRACOTTA))
                .legacy("stained_clay", (short) 2).get());
        loadMapping(Builder.of("light_blue_terracotta")
                .modern(LazyInit.of(() -> Material.LIGHT_BLUE_TERRACOTTA))
                .legacy("stained_clay", (short) 3).get());
        loadMapping(Builder.of("yellow_terracotta")
                .modern(LazyInit.of(() -> Material.YELLOW_TERRACOTTA))
                .legacy("stained_clay", (short) 4).get());
        loadMapping(Builder.of("lime_terracotta")
                .modern(LazyInit.of(() -> Material.LIME_TERRACOTTA))
                .legacy("stained_clay", (short) 5).get());
        loadMapping(Builder.of("pink_terracotta")
                .modern(LazyInit.of(() -> Material.PINK_TERRACOTTA))
                .legacy("stained_clay", (short) 6).get());
        loadMapping(Builder.of("gray_terracotta")
                .modern(LazyInit.of(() -> Material.GRAY_TERRACOTTA))
                .legacy("stained_clay", (short) 7).get());
        loadMapping(Builder.of("light_gray_terracotta")
                .modern(LazyInit.of(() -> Material.LIGHT_GRAY_TERRACOTTA))
                .legacy("stained_clay", (short) 8).get());
        loadMapping(Builder.of("cyan_terracotta")
                .modern(LazyInit.of(() -> Material.CYAN_TERRACOTTA))
                .legacy("stained_clay", (short) 9).get());
        loadMapping(Builder.of("purple_terracotta")
                .modern(LazyInit.of(() -> Material.PURPLE_TERRACOTTA))
                .legacy("stained_clay", (short) 10).get());
        loadMapping(Builder.of("blue_terracotta")
                .modern(LazyInit.of(() -> Material.BLUE_TERRACOTTA))
                .legacy("stained_clay", (short) 11).get());
        loadMapping(Builder.of("brown_terracotta")
                .modern(LazyInit.of(() -> Material.BROWN_TERRACOTTA))
                .legacy("stained_clay", (short) 12).get());
        loadMapping(Builder.of("green_terracotta")
                .modern(LazyInit.of(() -> Material.GREEN_TERRACOTTA))
                .legacy("stained_clay", (short) 13).get());
        loadMapping(Builder.of("red_terracotta")
                .modern(LazyInit.of(() -> Material.RED_TERRACOTTA))
                .legacy("stained_clay", (short) 14).get());
        loadMapping(Builder.of("black_terracotta")
                .modern(LazyInit.of(() -> Material.BLACK_TERRACOTTA))
                .legacy("stained_clay", (short) 15).get());

        // dyes
        loadMapping(Builder.of("black_dye")
                .modern(LazyInit.of(() -> Material.BLACK_DYE))
                .legacy("ink_sack").get());
        loadMapping(Builder.of("red_dye")
                .modern(LazyInit.of(() -> Material.RED_DYE))
                .legacy("ink_sack", (short) 1).get());
        loadMapping(Builder.of("green_dye")
                .modern(LazyInit.of(() -> Material.GREEN_DYE))
                .legacy("ink_sack", (short) 2).get());
        loadMapping(Builder.of("cocoa_beans")
                .modern(LazyInit.of(() -> Material.COCOA_BEANS))
                .legacy("ink_sack", (short) 3).get());
        loadMapping(Builder.of("lapis_lazuli")
                .modern(LazyInit.of(() -> Material.LAPIS_LAZULI))
                .legacy("ink_sack", (short) 4).get());
        loadMapping(Builder.of("purple_dye")
                .modern(LazyInit.of(() -> Material.PURPLE_DYE))
                .legacy("ink_sack", (short) 5).get());
        loadMapping(Builder.of("cyan_dye")
                .modern(LazyInit.of(() -> Material.CYAN_DYE))
                .legacy("ink_sack", (short) 6).get());
        loadMapping(Builder.of("light_gray_dye")
                .modern(LazyInit.of(() -> Material.LIGHT_GRAY_DYE))
                .legacy("ink_sack", (short) 7).get());
        loadMapping(Builder.of("gray_dye")
                .modern(LazyInit.of(() -> Material.GRAY_DYE))
                .legacy("ink_sack", (short) 8).get());
        loadMapping(Builder.of("pink_dye")
                .modern(LazyInit.of(() -> Material.PINK_DYE))
                .legacy("ink_sack", (short) 9).get());
        loadMapping(Builder.of("lime_dye")
                .modern(LazyInit.of(() -> Material.LIME_DYE))
                .legacy("ink_sack", (short) 10).get());
        loadMapping(Builder.of("yellow_dye")
                .modern(LazyInit.of(() -> Material.YELLOW_DYE))
                .legacy("ink_sack", (short) 11).get());
        loadMapping(Builder.of("light_blue_dye")
                .modern(LazyInit.of(() -> Material.LIGHT_BLUE_DYE))
                .legacy("ink_sack", (short) 12).get());
        loadMapping(Builder.of("magenta_dye")
                .modern(LazyInit.of(() -> Material.MAGENTA_DYE))
                .legacy("ink_sack", (short) 13).get());
        loadMapping(Builder.of("orange_dye")
                .modern(LazyInit.of(() -> Material.ORANGE_DYE))
                .legacy("ink_sack", (short) 14).get());
        loadMapping(Builder.of("bone_meal")
                .modern(LazyInit.of(() -> Material.BONE_MEAL))
                .legacy("ink_sack", (short) 15).get());

        // spawn eggs
        loadMapping(Builder.of("creeper_spawn_egg")
                .modern(LazyInit.of(() -> Material.CREEPER_SPAWN_EGG))
                .legacy("monster_egg", (short) 50).get());
        loadMapping(Builder.of("skeleton_spawn_egg")
                .modern(LazyInit.of(() -> Material.SKELETON_SPAWN_EGG))
                .legacy("monster_egg", (short) 51).get());
        loadMapping(Builder.of("spider_spawn_egg")
                .modern(LazyInit.of(() -> Material.SPIDER_SPAWN_EGG))
                .legacy("monster_egg", (short) 52).get());
        loadMapping(Builder.of("zombie_spawn_egg")
                .modern(LazyInit.of(() -> Material.ZOMBIE_SPAWN_EGG))
                .legacy("monster_egg", (short) 54).get());
        loadMapping(Builder.of("slime_spawn_egg")
                .modern(LazyInit.of(() -> Material.SLIME_SPAWN_EGG))
                .legacy("monster_egg", (short) 55).get());
        loadMapping(Builder.of("ghast_spawn_egg")
                .modern(LazyInit.of(() -> Material.GHAST_SPAWN_EGG))
                .legacy("monster_egg", (short) 56).get());
        loadMapping(Builder.of("zombified_piglin_spawn_egg")
                .modern(LazyInit.of(() -> Material.ZOMBIFIED_PIGLIN_SPAWN_EGG))
                .legacy("monster_egg", (short) 57).get());
        loadMapping(Builder.of("enderman_spawn_egg")
                .modern(LazyInit.of(() -> Material.ENDERMAN_SPAWN_EGG))
                .legacy("monster_egg", (short) 58).get());
        loadMapping(Builder.of("cave_spider_spawn_egg")
                .modern(LazyInit.of(() -> Material.CAVE_SPIDER_SPAWN_EGG))
                .legacy("monster_egg", (short) 59).get());
        loadMapping(Builder.of("silverfish_spawn_egg")
                .modern(LazyInit.of(() -> Material.SILVERFISH_SPAWN_EGG))
                .legacy("monster_egg", (short) 60).get());
        loadMapping(Builder.of("blaze_spawn_egg")
                .modern(LazyInit.of(() -> Material.BLAZE_SPAWN_EGG))
                .legacy("monster_egg", (short) 61).get());
        loadMapping(Builder.of("magma_cube_spawn_egg")
                .modern(LazyInit.of(() -> Material.MAGMA_CUBE_SPAWN_EGG))
                .legacy("monster_egg", (short) 62).get());
        loadMapping(Builder.of("bat_spawn_egg")
                .modern(LazyInit.of(() -> Material.BAT_SPAWN_EGG))
                .legacy("monster_egg", (short) 65).get());
        loadMapping(Builder.of("witch_spawn_egg")
                .modern(LazyInit.of(() -> Material.WITCH_SPAWN_EGG))
                .legacy("monster_egg", (short) 66).get());
        loadMapping(Builder.of("endermite_spawn_egg")
                .modern(LazyInit.of(() -> Material.ENDERMITE_SPAWN_EGG))
                .legacy("monster_egg", (short) 67).get());
        loadMapping(Builder.of("guardian_spawn_egg")
                .modern(LazyInit.of(() -> Material.GUARDIAN_SPAWN_EGG))
                .legacy("monster_egg", (short) 68).get());
        loadMapping(Builder.of("pig_spawn_egg")
                .modern(LazyInit.of(() -> Material.PIG_SPAWN_EGG))
                .legacy("monster_egg", (short) 90).get());
        loadMapping(Builder.of("sheep_spawn_egg")
                .modern(LazyInit.of(() -> Material.SHEEP_SPAWN_EGG))
                .legacy("monster_egg", (short) 91).get());
        loadMapping(Builder.of("cow_spawn_egg")
                .modern(LazyInit.of(() -> Material.COW_SPAWN_EGG))
                .legacy("monster_egg", (short) 92).get());
        loadMapping(Builder.of("chicken_spawn_egg")
                .modern(LazyInit.of(() -> Material.CHICKEN_SPAWN_EGG))
                .legacy("monster_egg", (short) 93).get());
        loadMapping(Builder.of("squid_spawn_egg")
                .modern(LazyInit.of(() -> Material.SQUID_SPAWN_EGG))
                .legacy("monster_egg", (short) 94).get());
        loadMapping(Builder.of("wolf_spawn_egg")
                .modern(LazyInit.of(() -> Material.WOLF_SPAWN_EGG))
                .legacy("monster_egg", (short) 95).get());
        loadMapping(Builder.of("mooshroom_spawn_egg")
                .modern(LazyInit.of(() -> Material.MOOSHROOM_SPAWN_EGG))
                .legacy("monster_egg", (short) 96).get());
        loadMapping(Builder.of("ocelot_spawn_egg")
                .modern(LazyInit.of(() -> Material.OCELOT_SPAWN_EGG))
                .legacy("monster_egg", (short) 98).get());
        loadMapping(Builder.of("horse_spawn_egg")
                .modern(LazyInit.of(() -> Material.HORSE_SPAWN_EGG))
                .legacy("monster_egg", (short) 100).get());
        loadMapping(Builder.of("rabbit_spawn_egg")
                .modern(LazyInit.of(() -> Material.RABBIT_SPAWN_EGG))
                .legacy("monster_egg", (short) 101).get());
        loadMapping(Builder.of("villager_spawn_egg")
                .modern(LazyInit.of(() -> Material.VILLAGER_SPAWN_EGG))
                .legacy("monster_egg", (short) 120).get());

        // skulls
        loadMapping(Builder.of("skull")
                .modern(LazyInit.of(() -> Material.PLAYER_HEAD))
                .legacy("skull").get());
        loadMapping(Builder.of("skeleton_skull")
                .modern(LazyInit.of(() -> Material.SKELETON_SKULL))
                .legacy("skull_item").get());
        loadMapping(Builder.of("wither_skeleton_skull")
                .modern(LazyInit.of(() -> Material.WITHER_SKELETON_SKULL))
                .legacy("skull_item", (short) 1).get());
        loadMapping(Builder.of("zombie_head")
                .modern(LazyInit.of(() -> Material.ZOMBIE_HEAD))
                .legacy("skull_item", (short) 2).get());
        loadMapping(Builder.of("player_head")
                .modern(LazyInit.of(() -> Material.PLAYER_HEAD))
                .legacy("skull_item", (short) 3).get());
        loadMapping(Builder.of("creeper_head")
                .modern(LazyInit.of(() -> Material.CREEPER_HEAD))
                .legacy("skull_item", (short) 4).get());

        // discs
        loadMapping(Builder.of("music_disc_13")
                .modern(LazyInit.of(() -> Material.MUSIC_DISC_13))
                .legacy("gold_record").get());
        loadMapping(Builder.of("music_disc_cat")
                .modern(LazyInit.of(() -> Material.MUSIC_DISC_CAT))
                .legacy("green_record").get());
        loadMapping(Builder.of("music_disc_blocks")
                .modern(LazyInit.of(() -> Material.MUSIC_DISC_BLOCKS))
                .legacy("record_3").get());
        loadMapping(Builder.of("music_disc_chirp")
                .modern(LazyInit.of(() -> Material.MUSIC_DISC_CHIRP))
                .legacy("record_4").get());
        loadMapping(Builder.of("music_disc_far")
                .modern(LazyInit.of(() -> Material.MUSIC_DISC_FAR))
                .legacy("record_5").get());
        loadMapping(Builder.of("music_disc_mall")
                .modern(LazyInit.of(() -> Material.MUSIC_DISC_MALL))
                .legacy("record_6").get());
        loadMapping(Builder.of("music_disc_mellohi")
                .modern(LazyInit.of(() -> Material.MUSIC_DISC_MELLOHI))
                .legacy("record_7").get());
        loadMapping(Builder.of("music_disc_stal")
                .modern(LazyInit.of(() -> Material.MUSIC_DISC_STAL))
                .legacy("record_8").get());
        loadMapping(Builder.of("music_disc_strad")
                .modern(LazyInit.of(() -> Material.MUSIC_DISC_STRAD))
                .legacy("record_9").get());
        loadMapping(Builder.of("music_disc_ward")
                .modern(LazyInit.of(() -> Material.MUSIC_DISC_WARD))
                .legacy("record_10").get());
        loadMapping(Builder.of("music_disc_11")
                .modern(LazyInit.of(() -> Material.MUSIC_DISC_11))
                .legacy("record_11").get());
        loadMapping(Builder.of("music_disc_wait")
                .modern(LazyInit.of(() -> Material.MUSIC_DISC_WAIT))
                .legacy("record_12").get());

        // slabs
        loadMapping(Builder.of("oak_slab")
                .modern(LazyInit.of(() -> Material.OAK_SLAB))
                .legacy("wood_step").get());
        loadMapping(Builder.of("spruce_slab")
                .modern(LazyInit.of(() -> Material.SPRUCE_SLAB))
                .legacy("wood_step", (short) 1).get());
        loadMapping(Builder.of("birch_slab")
                .modern(LazyInit.of(() -> Material.BIRCH_SLAB))
                .legacy("wood_step", (short) 2).get());
        loadMapping(Builder.of("jungle_slab")
                .modern(LazyInit.of(() -> Material.JUNGLE_SLAB))
                .legacy("wood_step", (short) 3).get());
        loadMapping(Builder.of("acacia_slab")
                .modern(LazyInit.of(() -> Material.ACACIA_SLAB))
                .legacy("wood_step", (short) 4).get());
        loadMapping(Builder.of("dark_oak_slab")
                .modern(LazyInit.of(() -> Material.DARK_OAK_SLAB))
                .legacy("wood_step", (short) 5).get());
        loadMapping(Builder.of("smooth_stone_slab")
                .modern(LazyInit.of(() -> Material.SMOOTH_STONE_SLAB))
                .legacy("step").get());
        loadMapping(Builder.of("sandstone_slab")
                .modern(LazyInit.of(() -> Material.SANDSTONE_SLAB))
                .legacy("step", (short) 1).get());
        loadMapping(Builder.of("cobblestone_slab")
                .modern(LazyInit.of(() -> Material.COBBLESTONE_SLAB))
                .legacy("step", (short) 3).get());
        loadMapping(Builder.of("brick_slab")
                .modern(LazyInit.of(() -> Material.BRICK_SLAB))
                .legacy("step", (short) 4).get());
        loadMapping(Builder.of("stone_brick_slab")
                .modern(LazyInit.of(() -> Material.STONE_BRICK_SLAB))
                .legacy("step", (short) 5).get());
        loadMapping(Builder.of("nether_brick_slab")
                .modern(LazyInit.of(() -> Material.NETHER_BRICK_SLAB))
                .legacy("step", (short) 6).get());
        loadMapping(Builder.of("quartz_slab")
                .modern(LazyInit.of(() -> Material.QUARTZ_SLAB))
                .legacy("step", (short) 7).get());
        loadMapping(Builder.of("red_sandstone_slab")
                .modern(LazyInit.of(() -> Material.RED_SANDSTONE_SLAB))
                .legacy("stone_slab2").get());

        // stairs
        loadMapping(Builder.of("oak_stairs")
                .modern(LazyInit.of(() -> Material.OAK_STAIRS))
                .legacy("wood_stairs").get());
        loadMapping(Builder.of("spruce_stairs")
                .modern(LazyInit.of(() -> Material.SPRUCE_STAIRS))
                .legacy("spruce_wood_stairs").get());
        loadMapping(Builder.of("birch_stairs")
                .modern(LazyInit.of(() -> Material.BIRCH_STAIRS))
                .legacy("birch_wood_stairs").get());
        loadMapping(Builder.of("jungle_stairs")
                .modern(LazyInit.of(() -> Material.JUNGLE_STAIRS))
                .legacy("jungle_wood_stairs").get());
        loadMapping(Builder.of("stone_brick_stairs")
                .modern(LazyInit.of(() -> Material.STONE_BRICK_STAIRS))
                .legacy("smooth_stairs").get());

        // tools
        loadMapping(Builder.of("iron_shovel")
                .modern(LazyInit.of(() -> Material.IRON_SHOVEL))
                .legacy("iron_spade").get());
        loadMapping(Builder.of("wooden_sword")
                .modern(LazyInit.of(() -> Material.WOODEN_SWORD))
                .legacy("wood_sword").get());
        loadMapping(Builder.of("wooden_shovel")
                .modern(LazyInit.of(() -> Material.WOODEN_SHOVEL))
                .legacy("wood_spade").get());
        loadMapping(Builder.of("wooden_pickaxe")
                .modern(LazyInit.of(() -> Material.WOODEN_PICKAXE))
                .legacy("wood_pickaxe").get());
        loadMapping(Builder.of("wooden_axe")
                .modern(LazyInit.of(() -> Material.WOODEN_AXE))
                .legacy("wood_axe").get());
        loadMapping(Builder.of("stone_shovel")
                .modern(LazyInit.of(() -> Material.STONE_SHOVEL))
                .legacy("stone_spade").get());
        loadMapping(Builder.of("diamond_shovel")
                .modern(LazyInit.of(() -> Material.DIAMOND_SHOVEL))
                .legacy("diamond_spade").get());
        loadMapping(Builder.of("mushroom_stew")
                .modern(LazyInit.of(() -> Material.MUSHROOM_STEW))
                .legacy("mushroom_soup").get());
        loadMapping(Builder.of("golden_sword")
                .modern(LazyInit.of(() -> Material.GOLDEN_SWORD))
                .legacy("gold_sword").get());
        loadMapping(Builder.of("golden_shovel")
                .modern(LazyInit.of(() -> Material.GOLDEN_SHOVEL))
                .legacy("gold_spade").get());
        loadMapping(Builder.of("golden_pickaxe")
                .modern(LazyInit.of(() -> Material.GOLDEN_PICKAXE))
                .legacy("gold_pickaxe").get());
        loadMapping(Builder.of("golden_axe")
                .modern(LazyInit.of(() -> Material.GOLDEN_AXE))
                .legacy("gold_axe").get());
        loadMapping(Builder.of("wooden_hoe")
                .modern(LazyInit.of(() -> Material.WOODEN_HOE))
                .legacy("wood_hoe").get());
        loadMapping(Builder.of("golden_hoe")
                .modern(LazyInit.of(() -> Material.GOLDEN_HOE))
                .legacy("gold_hoe").get());

        // armor
        loadMapping(Builder.of("golden_helmet")
                .modern(LazyInit.of(() -> Material.GOLDEN_HELMET))
                .legacy("gold_helmet").get());
        loadMapping(Builder.of("golden_chestplate")
                .modern(LazyInit.of(() -> Material.GOLDEN_CHESTPLATE))
                .legacy("gold_chestplate").get());
        loadMapping(Builder.of("golden_leggings")
                .modern(LazyInit.of(() -> Material.GOLDEN_LEGGINGS))
                .legacy("gold_leggings").get());
        loadMapping(Builder.of("golden_boots")
                .modern(LazyInit.of(() -> Material.GOLDEN_BOOTS))
                .legacy("gold_boots").get());


        loadMapping(Builder.of("oak_sign")
                .modern(LazyInit.of(() -> Material.OAK_SIGN))
                .legacy("sign").get());
        loadMapping(Builder.of("oak_wall_sign")
                .modern(LazyInit.of(() -> Material.OAK_WALL_SIGN))
                .legacy("wall_sign").get());
        loadMapping(Builder.of("snowball")
                .modern(LazyInit.of(() -> Material.SNOWBALL))
                .legacy("snow_ball").get());
        loadMapping(Builder.of("oak_boat")
                .modern(LazyInit.of(() -> Material.OAK_BOAT))
                .legacy("boat").get());
        loadMapping(Builder.of("bricks")
                .modern(LazyInit.of(() -> Material.BRICKS))
                .legacy("brick").get());
        loadMapping(Builder.of("chest_minecart")
                .modern(LazyInit.of(() -> Material.CHEST_MINECART))
                .legacy("storage_minecart").get());
        loadMapping(Builder.of("furnace_minecart")
                .modern(LazyInit.of(() -> Material.FURNACE_MINECART))
                .legacy("powered_minecart").get());
        loadMapping(Builder.of("clock")
                .modern(LazyInit.of(() -> Material.CLOCK))
                .legacy("watch").get());
        loadMapping(Builder.of("stone_bricks")
                .modern(LazyInit.of(() -> Material.STONE_BRICKS))
                .legacy("smooth_brick").get());
        loadMapping(Builder.of("mossy_stone_bricks")
                .modern(LazyInit.of(() -> Material.MOSSY_STONE_BRICKS))
                .legacy("smooth_brick", (short) 1).get());
        loadMapping(Builder.of("cracked_stone_bricks")
                .modern(LazyInit.of(() -> Material.CRACKED_STONE_BRICKS))
                .legacy("smooth_brick", (short) 2).get());
        loadMapping(Builder.of("chiseled_stone_bricks")
                .modern(LazyInit.of(() -> Material.CHISELED_STONE_BRICKS))
                .legacy("smooth_brick", (short) 3).get());
        loadMapping(Builder.of("prismarine_bricks")
                .modern(LazyInit.of(() -> Material.PRISMARINE_BRICKS))
                .legacy(Material.PRISMARINE, (short) 1).get());
        loadMapping(Builder.of("dark_prismarine")
                .modern(LazyInit.of(() -> Material.DARK_PRISMARINE))
                .legacy(Material.PRISMARINE, (short) 2).get());
        loadMapping(Builder.of("command_block")
                .modern(LazyInit.of(() -> Material.COMMAND_BLOCK))
                .legacy("command").get());
        loadMapping(Builder.of("cobblestone_wall")
                .modern(LazyInit.of(() -> Material.COBBLESTONE_WALL))
                .legacy("cobble_wall").get());
        loadMapping(Builder.of("mossy_cobblestone_wall")
                .modern(LazyInit.of(() -> Material.MOSSY_COBBLESTONE_WALL))
                .legacy("cobble_wall", (short) 1).get());
        loadMapping(Builder.of("oak_button")
                .modern(LazyInit.of(() -> Material.OAK_BUTTON))
                .legacy("wood_button").get());
        loadMapping(Builder.of("light_weighted_pressure_plate")
                .modern(LazyInit.of(() -> Material.LIGHT_WEIGHTED_PRESSURE_PLATE))
                .legacy("gold_plate").get());
        loadMapping(Builder.of("heavy_weighted_pressure_plate")
                .modern(LazyInit.of(() -> Material.HEAVY_WEIGHTED_PRESSURE_PLATE))
                .legacy("iron_plate").get());
        loadMapping(Builder.of("nether_quartz_ore")
                .modern(LazyInit.of(() -> Material.NETHER_QUARTZ_ORE))
                .legacy("quartz_ore").get());
        loadMapping(Builder.of("red_bed")
                .modern(LazyInit.of(() -> Material.RED_BED))
                .legacy("bed").get());

        // pistons
        loadMapping(Builder.of("sticky_piston")
                .modern(LazyInit.of(() -> Material.STICKY_PISTON))
                .legacy("piston_sticky_base").get());
        loadMapping(Builder.of("piston")
                .modern(LazyInit.of(() -> Material.PISTON))
                .legacy("piston_base").get());
        loadMapping(Builder.of("piston_head")
                .modern(LazyInit.of(() -> Material.PISTON_HEAD))
                .legacy("piston_extension").get());

        loadMapping(Builder.of("repeater")
                .modern(LazyInit.of(() -> Material.REPEATER))
                .legacy("diode").get());
        loadMapping(Builder.of("cobweb")
                .modern(LazyInit.of(() -> Material.COBWEB))
                .legacy("web").get());
        loadMapping(Builder.of("ink_sac")
                .modern(LazyInit.of(() -> Material.INK_SAC))
                .legacy("ink_sack").get());
        loadMapping(Builder.of("charcoal")
                .modern(LazyInit.of(() -> Material.CHARCOAL))
                .legacy(Material.COAL, (short) 1).get());
        loadMapping(Builder.of("gunpowder")
                .modern(LazyInit.of(() -> Material.GUNPOWDER))
                .legacy("sulphur").get());
        loadMapping(Builder.of("beef")
                .modern(LazyInit.of(() -> Material.BEEF))
                .legacy("raw_beef").get());
        loadMapping(Builder.of("chicken")
                .modern(LazyInit.of(() -> Material.CHICKEN))
                .legacy("raw_chicken").get());
        loadMapping(Builder.of("brewing_stand")
                .modern(LazyInit.of(() -> Material.BREWING_STAND))
                .legacy("brewing_stand_item").get());
        loadMapping(Builder.of("cauldron")
                .modern(LazyInit.of(() -> Material.CAULDRON))
                .legacy("cauldron_item").get());
        loadMapping(Builder.of("ender_eye")
                .modern(LazyInit.of(() -> Material.ENDER_EYE))
                .legacy("eye_of_ender").get());
        loadMapping(Builder.of("glistering_melon_slice")
                .modern(LazyInit.of(() -> Material.GLISTERING_MELON_SLICE))
                .legacy("speckled_melon").get());
        loadMapping(Builder.of("experience_bottle")
                .modern(LazyInit.of(() -> Material.EXPERIENCE_BOTTLE))
                .legacy("exp_bottle").get());
        loadMapping(Builder.of("writable_book")
                .modern(LazyInit.of(() -> Material.WRITABLE_BOOK))
                .legacy("book_and_quill").get());
        loadMapping(Builder.of("water")
                .modern(LazyInit.of(() -> Material.WATER))
                .legacy("stationary_water").get());
        loadMapping(Builder.of("lava")
                .modern(LazyInit.of(() -> Material.LAVA))
                .legacy("stationary_lava").get());
        loadMapping(Builder.of("flower_pot")
                .modern(LazyInit.of(() -> Material.FLOWER_POT))
                .legacy("flower_pot").get());
        loadMapping(Builder.of("carrot")
                .modern(LazyInit.of(() -> Material.CARROT))
                .legacy("carrot").get());
        loadMapping(Builder.of("potato")
                .modern(LazyInit.of(() -> Material.POTATO))
                .legacy("potato").get());
        loadMapping(Builder.of("map")
                .modern(LazyInit.of(() -> Material.MAP))
                .legacy("map").get());
        loadMapping(Builder.of("chiseled_quartz_block")
                .modern(LazyInit.of(() -> Material.CHISELED_QUARTZ_BLOCK))
                .legacy(Material.QUARTZ_BLOCK, (short) 1).get());
        loadMapping(Builder.of("quartz_pillar")
                .modern(LazyInit.of(() -> Material.QUARTZ_PILLAR))
                .legacy(Material.QUARTZ_BLOCK, (short) 2).get());
        loadMapping(Builder.of("carrot_on_a_stick")
                .modern(LazyInit.of(() -> Material.CARROT_ON_A_STICK))
                .legacy("carrot_stick").get());
        loadMapping(Builder.of("comparator")
                .modern(LazyInit.of(() -> Material.COMPARATOR))
                .legacy("redstone_comparator").get());
        loadMapping(Builder.of("nether_brick")
                .modern(LazyInit.of(() -> Material.NETHER_BRICK))
                .legacy("nether_brick_item").get());
        loadMapping(Builder.of("nether_bricks")
                .modern(LazyInit.of(() -> Material.NETHER_BRICKS))
                .legacy("nether_brick").get());
        loadMapping(Builder.of("firework_rocket")
                .modern(LazyInit.of(() -> Material.FIREWORK_ROCKET))
                .legacy("firework").get());
        loadMapping(Builder.of("fire_charge")
                .modern(LazyInit.of(() -> Material.FIRE_CHARGE))
                .legacy("firework_charge").get());
        loadMapping(Builder.of("firework_star")
                .modern(LazyInit.of(() -> Material.FIREWORK_STAR))
                .legacy("fireball").get());
        loadMapping(Builder.of("tnt_minecart")
                .modern(LazyInit.of(() -> Material.TNT_MINECART))
                .legacy("explosive_minecart").get());
        loadMapping(Builder.of("oak_fence_gate")
                .modern(LazyInit.of(() -> Material.OAK_FENCE_GATE))
                .legacy("fence_gate").get());
        loadMapping(Builder.of("iron_horse_armor")
                .modern(LazyInit.of(() -> Material.IRON_HORSE_ARMOR))
                .legacy("iron_barding").get());
        loadMapping(Builder.of("golden_horse_armor")
                .modern(LazyInit.of(() -> Material.GOLDEN_HORSE_ARMOR))
                .legacy("gold_barding").get());
        loadMapping(Builder.of("diamond_horse_armor")
                .modern(LazyInit.of(() -> Material.DIAMOND_HORSE_ARMOR))
                .legacy("diamond_barding").get());
        loadMapping(Builder.of("lead")
                .modern(LazyInit.of(() -> Material.LEAD))
                .legacy("leash").get());
        loadMapping(Builder.of("nether_wart")
                .modern(LazyInit.of(() -> Material.NETHER_WART))
                .legacy("nether_stalk").get());
        loadMapping(Builder.of("spawner")
                .modern(LazyInit.of(() -> Material.SPAWNER))
                .legacy("mob_spawner").get());
        loadMapping(Builder.of("crafting_table")
                .modern(LazyInit.of(() -> Material.CRAFTING_TABLE))
                .legacy("workbench").get());
        loadMapping(Builder.of("soul_soil")
                .modern(LazyInit.of(() -> Material.SOUL_SOIL))
                .legacy("soil").get());
        loadMapping(Builder.of("rail")
                .modern(LazyInit.of(() -> Material.RAIL))
                .legacy("rails").get());
        loadMapping(Builder.of("stone_pressure_plate")
                .modern(LazyInit.of(() -> Material.STONE_PRESSURE_PLATE))
                .legacy("stone_plate").get());
        loadMapping(Builder.of("iron_door")
                .modern(LazyInit.of(() -> Material.IRON_DOOR))
                .legacy("iron_door_block").get());
        loadMapping(Builder.of("oak_pressure_plate")
                .modern(LazyInit.of(() -> Material.OAK_PRESSURE_PLATE))
                .legacy("wood_plate").get());
        loadMapping(Builder.of("redstone_torch")
                .modern(LazyInit.of(() -> Material.REDSTONE_TORCH))
                .legacy("redstone_torch_on").get());
        loadMapping(Builder.of("oak_trapdoor")
                .modern(LazyInit.of(() -> Material.OAK_TRAPDOOR))
                .legacy("trap_door").get());
        loadMapping(Builder.of("porkchop")
                .modern(LazyInit.of(() -> Material.PORKCHOP))
                .legacy("pork").get());
        loadMapping(Builder.of("cooked_porkchop")
                .modern(LazyInit.of(() -> Material.COOKED_PORKCHOP))
                .legacy("grilled_pork").get());
        loadMapping(Builder.of("golden_apple")
                .modern(LazyInit.of(() -> Material.GOLDEN_APPLE))
                .legacy("golden_apple").get());
        loadMapping(Builder.of("enchanted_golden_apple")
                .modern(LazyInit.of(() -> Material.ENCHANTED_GOLDEN_APPLE))
                .legacy("golden_apple", (short) 1).get());
        loadMapping(Builder.of("cod")
                .modern(LazyInit.of(() -> Material.COD))
                .legacy("raw_fish").get());
        loadMapping(Builder.of("salmon")
                .modern(LazyInit.of(() -> Material.SALMON))
                .legacy("raw_fish", (short) 1).get());
        loadMapping(Builder.of("tropical_fish")
                .modern(LazyInit.of(() -> Material.TROPICAL_FISH))
                .legacy("raw_fish", (short) 2).get());
        loadMapping(Builder.of("pufferfish")
                .modern(LazyInit.of(() -> Material.PUFFERFISH))
                .legacy("raw_fish", (short) 3).get());
        loadMapping(Builder.of("cooked_cod")
                .modern(LazyInit.of(() -> Material.COOKED_COD))
                .legacy("cooked_fish").get());
        loadMapping(Builder.of("cooked_salmon")
                .modern(LazyInit.of(() -> Material.COOKED_SALMON))
                .legacy("cooked_fish", (short) 1).get());
        loadMapping(Builder.of("melon")
                .modern(LazyInit.of(() -> Material.MELON))
                .legacy("melon_block").get());
        loadMapping(Builder.of("melon_slice")
                .modern(LazyInit.of(() -> Material.MELON_SLICE))
                .legacy("melon").get());
        loadMapping(Builder.of("iron_bars")
                .modern(LazyInit.of(() -> Material.IRON_BARS))
                .legacy("iron_fence").get());
        loadMapping(Builder.of("glass_pane")
                .modern(LazyInit.of(() -> Material.GLASS_PANE))
                .legacy("thin_glass").get());
        loadMapping(Builder.of("mycelium")
                .modern(LazyInit.of(() -> Material.MYCELIUM))
                .legacy("mycel").get());
        loadMapping(Builder.of("lily_pad")
                .modern(LazyInit.of(() -> Material.LILY_PAD))
                .legacy("water_lily").get());
        loadMapping(Builder.of("nether_brick_fence")
                .modern(LazyInit.of(() -> Material.NETHER_BRICK_FENCE))
                .legacy("nether_fence").get());
        loadMapping(Builder.of("enchanting_table")
                .modern(LazyInit.of(() -> Material.ENCHANTING_TABLE))
                .legacy("enchantment_table").get());
        loadMapping(Builder.of("end_portal_frame")
                .modern(LazyInit.of(() -> Material.END_PORTAL_FRAME))
                .legacy("ender_portal_frame").get());
        loadMapping(Builder.of("end_stone")
                .modern(LazyInit.of(() -> Material.END_STONE))
                .legacy("ender_stone").get());
        loadMapping(Builder.of("redstone_lamp")
                .modern(LazyInit.of(() -> Material.REDSTONE_LAMP))
                .legacy("redstone_lamp_off").get());
        loadMapping(Builder.of("chiseled_sandstone")
                .modern(LazyInit.of(() -> Material.CHISELED_SANDSTONE))
                .legacy(Material.SANDSTONE, (short) 1).get());
        loadMapping(Builder.of("smooth_sandstone")
                .modern(LazyInit.of(() -> Material.SMOOTH_SANDSTONE))
                .legacy(Material.SANDSTONE, (short) 2).get());
        loadMapping(Builder.of("chiseled_red_sandstone")
                .modern(LazyInit.of(() -> Material.CHISELED_RED_SANDSTONE))
                .legacy(Material.RED_SANDSTONE, (short) 1).get());
        loadMapping(Builder.of("smooth_red_sandstone")
                .modern(LazyInit.of(() -> Material.SMOOTH_RED_SANDSTONE))
                .legacy(Material.RED_SANDSTONE, (short) 2).get());
        loadMapping(Builder.of("wet_sponge")
                .modern(LazyInit.of(() -> Material.WET_SPONGE))
                .legacy(Material.SPONGE, (short) 1).get());
        loadMapping(Builder.of("burning_furnace")
                .modern(LazyInit.of(() -> Material.LEGACY_BURNING_FURNACE)) // FURNACE
                .legacy("burning_furnace").get());
    }

    public static class Builder {

        private final String identifier;
        private ItemSupplier legacy;
        private ItemSupplier modern;

        public Builder(String identifier) {
            this.identifier = identifier;
        }

        public static Builder of(String identifier) {
            return new ItemMappings.Builder(identifier);
        }

        public Builder modern(LazyInit<Material> material) {
            this.modern = ItemMappings.getModern(material);
            return this;
        }

        public Builder modern(Material material) {
            return modern(LazyInit.of(() -> material));
        }

        public Builder legacy(LazyInit<Material> material, short data) {
            this.legacy = ItemMappings.getLegacy(material, data);
            return this;
        }

        public Builder legacy(Material material, short data) {
            return legacy(LazyInit.of(() -> material), data);
        }

        public Builder legacy(String material, short data) {
            return legacy(LazyInit.of(() -> Material.getMaterial(material.toUpperCase())), data);
        }

        public Builder legacy(String material) {
            return legacy(material, (short) 0);
        }

        public MappingData get() {
            return new MappingData(identifier, ItemMappings.get(legacy, modern));
        }
    }

    public static class MappingData {
        private final String identifier;
        private final ItemSupplier supplier;

        public MappingData(String identifier, ItemSupplier supplier) {
            this.identifier = identifier;
            this.supplier = supplier;
        }

        public String getIdentifier() {
            return identifier;
        }

        public ItemSupplier get() {
            return supplier;
        }
    }
}