package dk.superawesome.factorio.util.helper;

import dk.superawesome.factorio.Factorio;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemBuilder {

    private static final Enchantment glowEnchantment;

    static {
        glowEnchantment = new Enchantment() {
            @Override
            public String getName() {
                return "Glow";
            }

            @Override
            public int getMaxLevel() {
                return 2;
            }

            @Override
            public int getStartLevel() {
                return 1;
            }

            @Override
            public EnchantmentTarget getItemTarget() {
                return EnchantmentTarget.ARMOR;
            }

            @Override
            public boolean isTreasure() {
                return false;
            }

            @Override
            public boolean isCursed() {
                return false;
            }

            @Override
            public boolean conflictsWith(Enchantment enchantment) {
                return false;
            }

            @Override
            public boolean canEnchantItem(ItemStack itemStack) {
                return true;
            }

            @Override
            public NamespacedKey getKey() {
                return new NamespacedKey(Factorio.get(), "glow");
            }

            @Override
            public String getTranslationKey() {
                return "glow";
            }
        };
    }

    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    public ItemBuilder(Material material) {
        itemStack = new ItemStack(material);
        itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.itemMeta = itemStack.getItemMeta();
    }


    public ItemBuilder setName(String itemName) {
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemName));
        updateMeta();
        return this;
    }

    public ItemBuilder setLore(String[] lores) {
        for(int i = 0; i <lores.length; i++) {
            lores[i] = ChatColor.translateAlternateColorCodes('&', lores[i]);
        }
        itemMeta.setLore(Arrays.asList(lores).stream().flatMap((s) -> Stream.of( s.split( "\\r?\\n" ) )).collect(Collectors.toList()));
        updateMeta();
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder addFlags(ItemFlag... flags) {
        itemMeta.addItemFlags(flags);
        updateMeta();
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchantment, int level) {
        itemMeta.addEnchant(enchantment, level, true);
        updateMeta();
        return this;
    }

    public ItemBuilder makeGlowing() {
        addEnchant(glowEnchantment, 1);
        addFlags(ItemFlag.HIDE_ENCHANTS);
        updateMeta();
        return this;
    }

    private void updateMeta() {
        itemStack.setItemMeta(itemMeta);
    }

    public ItemStack build() {
        return itemStack;
    }
}
