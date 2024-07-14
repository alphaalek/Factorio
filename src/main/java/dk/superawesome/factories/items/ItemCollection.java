package dk.superawesome.factories.items;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public interface ItemCollection {

    static ItemCollection from(Inventory inventory) {
        return new ItemCollection() {
            @Override
            public boolean has(ItemStack stack) {
                return inventory.contains(stack);
            }

            @Override
            public List<ItemStack> take(ItemStack stack) {
                List<ItemStack> stacks = new ArrayList<>();
                for (ItemStack content : inventory.getContents()) {
                    // check if this inventory slot item matches the item we want to take
                    if (content != null && content.isSimilar(stack)) {
                        ItemStack req = content.clone();

                        // only allow the amount of items we require
                        if (req.getAmount() > stack.getAmount()) {
                            req.setAmount(stack.getAmount());
                        }

                        content.setAmount(content.getAmount() - req.getAmount());
                        stacks.add(req);
                    }

                    // return if all required items have been found and taken
                    if (stack.getAmount() == 0) {
                        return stacks;
                    }
                }

                return stacks;
            }
        };
    }

    boolean has(ItemStack stack);

    List<ItemStack> take(ItemStack stack);
}
