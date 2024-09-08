package dk.superawesome.factorio.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;

public class MaterialTags {

    public static final Map<String, Tag<Material>> LIST = new HashMap<>();

    static {
        try {
            Class<?> clazz = Class.forName(Tag.class.getName());
            for (Field field : clazz.getDeclaredFields()) {
                Object val = field.get(null);
                if (val instanceof Tag<?> tag) {
                    if (Arrays.stream(((ParameterizedType) tag.getClass().getGenericSuperclass()).getActualTypeArguments())
                            .map(Type::getTypeName)
                            .anyMatch(t -> Material.class.getName().equals(t))) {
                        LIST.put(field.getName().toLowerCase(), (Tag<Material>) tag);
                    }
                }
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to acquire material tags", ex);
        }
    }
}
