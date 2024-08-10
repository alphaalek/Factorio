package dk.superawesome.factorio.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TagVerifier {

    private static final List<Tag<Material>> materialTags = new ArrayList<>();

    static {
        try {
            Class<?> clazz = Class.forName(Tag.class.getName());
            for (Field field : clazz.getDeclaredFields()) {
                Object val = field.get(null);
                if (val instanceof Tag<?> tag) {
                    if (Arrays.stream(((ParameterizedType) tag.getClass().getGenericSuperclass()).getActualTypeArguments())
                            .map(Type::getTypeName)
                            .anyMatch(t -> Material.class.getName().equals(t))) {
                        materialTags.add((Tag<Material>) tag);
                    }
                }
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to acquire material tags", ex);
        }
    }

    public static boolean checkHighestTag(ItemStack ingredient, ItemStack offer) {
        if (ingredient != null && offer != null && ingredient.hasItemMeta()) {
            Material mat = ingredient.getType();
            Material offerMat = offer.getType();

            // get all materials from the tags where the ingredient is tagged
            List<Material> materials = new ArrayList<>();
            for (Tag<Material> tag : materialTags) {
                if (tag.isTagged(mat)) {
                    materials.addAll(tag.getValues());
                }
            }

            // ... get the material occurring the most of the values from these tags
            List<Map.Entry<Material, Long>> mostOccur = materials.stream().collect(Collectors.groupingBy(i -> i, Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .toList();

            // TODO: tag registry, instead of calculating this stuff

            long prev = 0;
            for (Map.Entry<Material, Long> entry : mostOccur) {
                if (entry.getValue() >= prev) {
                    prev = entry.getValue();
                    if (offerMat == entry.getKey()) {
                        return true;
                    }
                    continue;
                }
                return false;
            }
        }

        return false;
    }
}
