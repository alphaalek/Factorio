package dk.superawesome.factories.util.helper;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemBuilder {

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


    public ItemBuilder setItemName(String itemName) {
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemName));
        updateMeta();
        return this;
    }

    public ItemBuilder setLore(String[] lores) {
        for(int i = 0;i<lores.length;i++) {
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

    public ItemBuilder addFlags(ItemFlag[] flags) {
        itemMeta.addItemFlags(flags);
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
