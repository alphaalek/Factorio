package dk.superawesome.factorio.util.helper;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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


    public ItemBuilder setName(String itemName) {
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemName));
        updateMeta();
        return this;
    }

    public ItemBuilder addLore(String line) {
        return changeLore(l -> l.add(line));
    }

    public ItemBuilder changeLore(Consumer<List<String>> doLore) {
        List<String> lore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
        doLore.accept(lore);
        itemMeta.setLore(lore);
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
        addEnchant(Enchantment.LURE, 1);
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
