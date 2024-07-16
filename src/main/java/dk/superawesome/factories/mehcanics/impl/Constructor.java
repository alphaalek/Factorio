package dk.superawesome.factories.mehcanics.impl;

import dk.superawesome.factories.gui.impl.ConstructorGui;
import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.mehcanics.AbstractMechanic;
import dk.superawesome.factories.mehcanics.MechanicProfile;
import dk.superawesome.factories.mehcanics.Profiles;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Constructor extends AbstractMechanic<Constructor, ConstructorGui> {

    private final ItemStack[] craftingGridItems = new ItemStack[9];
    private ItemStack recipeResult;

    public Constructor(Location loc) {
        super(loc);
    }

    public ItemStack[] getCraftingGridItems() {
        return craftingGridItems;
    }

    public ItemStack getRecipeResult() {
        return recipeResult;
    }

    public void setRecipeResult(ItemStack result) {
        this.recipeResult = result;
    }

    @Override
    public MechanicProfile<Constructor, ConstructorGui> getProfile() {
        return Profiles.CONSTRUCTOR;
    }

    @Override
    public void pipePut(ItemCollection collection) {
        if (tickThrottle.isThrottled()) {
            return;
        }

        for (ItemStack craft : craftingGridItems) {
            // check if this slot contains anything and can hold more
            if (craft == null || craft.getAmount() == craft.getMaxStackSize()) {
                continue;
            }

            // poll the item for this crafting slot from the item collection
            ItemStack req = craft.clone();
            req.setAmount(1);

            if (collection.has(req)) {
                List<ItemStack> stacks = collection.take(req.getAmount());
                if (!stacks.isEmpty() && stacks.get(0).isSimilar(craft)) {
                    craft.setAmount(Math.min(craft.getMaxStackSize(), craft.getAmount() + stacks.get(0).getAmount()));
                }
            }
        }
    }
}
